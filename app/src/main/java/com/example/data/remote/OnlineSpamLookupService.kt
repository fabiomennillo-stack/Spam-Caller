package com.example.data.remote

import com.example.data.local.SpamLookupCacheEntity
import com.example.data.local.SpamSourceEntity
import com.example.data.local.SpamcallerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class SpamVerdict(val labelIt: String) {
    SAFE("Sicuro"),
    SUSPICIOUS("Sospetto"),
    HIGH_RISK("Spam / Pericoloso"),
    UNKNOWN("Nessuna segnalazione")
}

data class SourceLookupResult(
    val sourceName: String,
    val url: String,
    val reachable: Boolean,
    val httpCode: Int,
    val detectedScore: Int,
    val reportsFound: Int,
    val matchedTags: List<String>,
    val snippet: String,
    val latencyMs: Long
)

data class OnlineSpamReport(
    val normalizedNumber: String,
    val riskScore: Int,
    val verdict: SpamVerdict,
    val totalReports: Int,
    val summary: String,
    val isManuallyBlocked: Boolean,
    val sources: List<SourceLookupResult>,
    val fetchedAt: Long
)

class OnlineSpamLookupService(
    private val dao: SpamcallerDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun ensureDefaultSourcesSeeded() {
        if (dao.countSpamSources() == 0) {
            dao.insertSpamSources(
                com.example.data.local.SpamcallerDatabase.DEFAULT_SPAM_SOURCES
            )
        }
    }

    suspend fun lookupNumber(
        rawNumber: String,
        forceRefresh: Boolean = false
    ): OnlineSpamReport = withContext(Dispatchers.IO) {
        ensureDefaultSourcesSeeded()
        val normalized = normalizePhoneNumber(rawNumber)
        val blocked = dao.getBlockedNumber(normalized)

        if (!forceRefresh) {
            val cached = dao.getLookupCache(normalized)
            val ageMs = System.currentTimeMillis() - (cached?.fetchedAt ?: 0L)
            if (cached != null && ageMs < 6 * 60 * 60 * 1000L) {
                return@withContext mapCacheToReport(cached, blocked != null)
            }
        }

        val enabledSources = dao.getEnabledSpamSourcesOnce()
        val queryDigits = formatNumberForWebQuery(normalized)

        val results = coroutineScope {
            enabledSources.map { source ->
                async { querySingleWebsite(source, queryDigits) }
            }.awaitAll()
        }

        val reachableResults = results.filter { it.reachable }
        val totalReports = results.sumOf { it.reportsFound }
        val weightedRisk = computeAggregateRisk(results, blocked != null)
        val verdict = when {
            blocked != null || weightedRisk >= 65 -> SpamVerdict.HIGH_RISK
            weightedRisk >= 35 -> SpamVerdict.SUSPICIOUS
            reachableResults.isNotEmpty() && weightedRisk <= 15 && results.any {
                it.matchedTags.any { tag -> tag == "affidabile" || tag == "sicuro" || tag == "positivo" }
            } -> SpamVerdict.SAFE
            reachableResults.isNotEmpty() -> SpamVerdict.UNKNOWN
            else -> SpamVerdict.UNKNOWN
        }

        val topTags = results.flatMap { it.matchedTags }.distinct().take(4)
        val summary = buildString {
            if (blocked != null) {
                append("Numero presente nella tua lista nera (${blocked.reason}). ")
            }
            if (reachableResults.isEmpty()) {
                append("Impossibile raggiungere i siti configurati al momento.")
            } else if (topTags.isNotEmpty()) {
                append("Segnalazioni trovate su ${reachableResults.size} siti: ${topTags.joinToString(", ")}.")
            } else if (totalReports > 0) {
                append("$totalReports riscontri trovati sui database telefonici online.")
            } else {
                append("Controllati ${reachableResults.size} siti online: nessuna segnalazione spam rilevata.")
            }
        }.trim()

        val breakdownJson = encodeBreakdown(results)
        val now = System.currentTimeMillis()

        dao.upsertLookupCache(
            SpamLookupCacheEntity(
                normalizedNumber = normalized,
                riskScore = weightedRisk,
                verdict = verdict.name,
                reportCount = totalReports,
                summary = summary,
                sourceBreakdownJson = breakdownJson,
                fetchedAt = now
            )
        )

        OnlineSpamReport(
            normalizedNumber = normalized,
            riskScore = weightedRisk,
            verdict = verdict,
            totalReports = totalReports,
            summary = summary,
            isManuallyBlocked = blocked != null,
            sources = results,
            fetchedAt = now
        )
    }

    private fun querySingleWebsite(source: SpamSourceEntity, numberForUrl: String): SourceLookupResult {
        val encoded = URLEncoder.encode(numberForUrl, "UTF-8")
        val url = if (source.urlTemplate.contains("{number}")) {
            source.urlTemplate.replace("{number}", encoded)
        } else {
            source.urlTemplate.trimEnd('/') + "/" + encoded
        }

        val start = System.currentTimeMillis()
        return try {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "it-IT,it;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val body = response.body?.string()?.take(180_000) ?: ""
                if (!response.isSuccessful && response.code != 404) {
                    return SourceLookupResult(
                        sourceName = source.name,
                        url = url,
                        reachable = false,
                        httpCode = response.code,
                        detectedScore = 0,
                        reportsFound = 0,
                        matchedTags = emptyList(),
                        snippet = "Risposta HTTP ${response.code}",
                        latencyMs = latency
                    )
                }

                parseHtmlSignals(
                    source = source,
                    url = url,
                    httpCode = response.code,
                    rawHtml = body,
                    latency = latency
                )
            }
        } catch (e: Exception) {
            SourceLookupResult(
                sourceName = source.name,
                url = url,
                reachable = false,
                httpCode = 0,
                detectedScore = 0,
                reportsFound = 0,
                matchedTags = emptyList(),
                snippet = e.localizedMessage ?: "Errore di connessione",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    internal fun parseHtmlSignals(
        source: SpamSourceEntity,
        url: String,
        httpCode: Int,
        rawHtml: String,
        latency: Long
    ): SourceLookupResult {
        val cleaned = rawHtml
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

        val lower = cleaned.lowercase(Locale.ITALIAN)
        val keywords = source.keywordsCsv
            .split(",")
            .map { it.trim().lowercase(Locale.ITALIAN) }
            .filter { it.isNotBlank() }

        val negativeVocab = (keywords + listOf(
            "spam", "truffa", "telemarketing", "call center", "molesto",
            "pericoloso", "phishing", "aggressivo", "sondaggio", "indesiderato",
            "pubblicita", "pubblicità", "scocciatore", "negativo", "truffatori"
        )).distinct()

        val positiveVocab = listOf("affidabile", "sicuro", "positivo", "legittimo", "corriere")

        val matchedNegative = negativeVocab.filter { kw ->
            Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(lower)
        }
        val matchedPositive = positiveVocab.filter { kw ->
            Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(lower)
        }

        // Extract Tellows-style score (e.g., "Score 8", "tellows score: 7", "valutazione 8/9")
        val tellowsScoreMatch = Regex(
            "(?:tellows\\s*score|score|punteggio|valutazione)\\D{0,12}([1-9])(?:\\s*/\\s*9)?",
            RegexOption.IGNORE_CASE
        ).find(cleaned)
        val tellowsScale = tellowsScoreMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

        // Extract comment/report count
        val reportsMatch = Regex(
            "(\\d{1,4})\\s*(?:commenti|segnalazioni|valutazioni|voti|ricerche|recensioni)",
            RegexOption.IGNORE_CASE
        ).find(cleaned)
        val reportsCount = reportsMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

        var risk = when {
            tellowsScale != null && tellowsScale >= 7 -> 70 + (tellowsScale - 7) * 12
            tellowsScale != null && tellowsScale >= 5 -> 45
            tellowsScale != null && tellowsScale <= 3 -> 10
            else -> 0
        }

        if (matchedNegative.isNotEmpty()) {
            risk = maxOf(risk, (35 + matchedNegative.size * 15).coerceAtMost(95))
        }
        if (reportsCount >= 10 && matchedNegative.isNotEmpty()) {
            risk = (risk + 12).coerceAtMost(98)
        }
        if (matchedPositive.isNotEmpty() && matchedNegative.isEmpty() && tellowsScale == null) {
            risk = 10
        }
        if (httpCode == 404) {
            risk = 0
        }

        val snippet = when {
            httpCode == 404 -> "Numero non ancora presente in questo sito."
            matchedNegative.isNotEmpty() -> {
                val firstKw = matchedNegative.first()
                val idx = lower.indexOf(firstKw)
                val from = (idx - 40).coerceAtLeast(0)
                val to = (idx + 120).coerceAtMost(cleaned.length)
                "…${cleaned.substring(from, to).trim()}…"
            }
            cleaned.length > 30 -> cleaned.take(140)
            else -> "Pagina consultata senza dettagli testuali."
        }

        return SourceLookupResult(
            sourceName = source.name,
            url = url,
            reachable = true,
            httpCode = httpCode,
            detectedScore = risk,
            reportsFound = reportsCount,
            matchedTags = (matchedNegative + matchedPositive).distinct().take(5),
            snippet = snippet,
            latencyMs = latency
        )
    }

    private fun computeAggregateRisk(
        results: List<SourceLookupResult>,
        isManuallyBlocked: Boolean
    ): Int {
        if (isManuallyBlocked) return 100
        val nonZero = results.filter { it.reachable && it.detectedScore > 0 }
        if (nonZero.isEmpty()) return 0
        val maxScore = nonZero.maxOf { it.detectedScore }
        val avgScore = nonZero.map { it.detectedScore }.average().toInt()
        return ((maxScore * 0.65) + (avgScore * 0.35)).toInt().coerceIn(0, 100)
    }

    private fun encodeBreakdown(results: List<SourceLookupResult>): String {
        val arr = JSONArray()
        results.forEach { r ->
            arr.put(JSONObject().apply {
                put("sourceName", r.sourceName)
                put("url", r.url)
                put("reachable", r.reachable)
                put("httpCode", r.httpCode)
                put("detectedScore", r.detectedScore)
                put("reportsFound", r.reportsFound)
                put("matchedTags", JSONArray(r.matchedTags))
                put("snippet", r.snippet)
                put("latencyMs", r.latencyMs)
            })
        }
        return arr.toString()
    }

    private fun mapCacheToReport(
        cached: SpamLookupCacheEntity,
        isManuallyBlocked: Boolean
    ): OnlineSpamReport {
        val list = mutableListOf<SourceLookupResult>()
        val arr = runCatching { JSONArray(cached.sourceBreakdownJson) }.getOrNull() ?: JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val tagsArr = obj.optJSONArray("matchedTags") ?: JSONArray()
            val tags = buildList {
                for (t in 0 until tagsArr.length()) {
                    add(tagsArr.optString(t))
                }
            }
            list.add(
                SourceLookupResult(
                    sourceName = obj.optString("sourceName"),
                    url = obj.optString("url"),
                    reachable = obj.optBoolean("reachable"),
                    httpCode = obj.optInt("httpCode"),
                    detectedScore = obj.optInt("detectedScore"),
                    reportsFound = obj.optInt("reportsFound"),
                    matchedTags = tags,
                    snippet = obj.optString("snippet"),
                    latencyMs = obj.optLong("latencyMs")
                )
            )
        }
        val verdict = runCatching { SpamVerdict.valueOf(cached.verdict) }
            .getOrDefault(SpamVerdict.UNKNOWN)
        return OnlineSpamReport(
            normalizedNumber = cached.normalizedNumber,
            riskScore = if (isManuallyBlocked) 100 else cached.riskScore,
            verdict = if (isManuallyBlocked) SpamVerdict.HIGH_RISK else verdict,
            totalReports = cached.reportCount,
            summary = cached.summary,
            isManuallyBlocked = isManuallyBlocked,
            sources = list,
            fetchedAt = cached.fetchedAt
        )
    }

    companion object {
        fun normalizePhoneNumber(raw: String): String {
            val trimmed = raw.trim()
            val hasPlus = trimmed.startsWith("+")
            val digits = trimmed.filter { it.isDigit() }
            if (digits.isEmpty()) return trimmed
            return when {
                digits.startsWith("00") -> "+${digits.drop(2)}"
                hasPlus -> "+$digits"
                else -> digits
            }
        }

        fun formatNumberForWebQuery(normalized: String): String {
            // Strip +39 or leading + for Italian directory sites when appropriate
            return when {
                normalized.startsWith("+39") && normalized.length > 5 -> normalized.removePrefix("+39")
                normalized.startsWith("+") -> normalized.removePrefix("+")
                else -> normalized
            }
        }
    }
}
