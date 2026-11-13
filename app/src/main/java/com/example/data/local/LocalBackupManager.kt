package com.example.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhoneBackupSummary(
    val fileName: String,
    val filePath: String,
    val blockedCount: Int,
    val sourcesCount: Int,
    val cacheCount: Int,
    val sizeBytes: Long
)

data class RestoreSummary(
    val blockedRestored: Int,
    val sourcesRestored: Int,
    val cacheRestored: Int
)

class LocalBackupManager(
    private val context: Context,
    private val dao: SpamcallerDao
) {
    private val backupDir: File
        get() = File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }

    suspend fun createBackupPayloadJson(
        autoBlockEnabled: Boolean,
        spamThreshold: Int,
        blockHiddenNumbers: Boolean
    ): Pair<String, Triple<Int, Int, Int>> = withContext(Dispatchers.IO) {
        val blocked = dao.getAllBlockedNumbersOnce()
        val sources = dao.getAllSpamSourcesOnce()
        val caches = dao.getAllLookupCacheOnce()

        val root = JSONObject().apply {
            put("app", "Spamcaller")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("autoBlockEnabled", autoBlockEnabled)
                put("spamThreshold", spamThreshold)
                put("blockHiddenNumbers", blockHiddenNumbers)
            })
            put("blockedNumbers", JSONArray().apply {
                blocked.forEach { b ->
                    put(JSONObject().apply {
                        put("phoneNumber", b.phoneNumber)
                        put("displayName", b.displayName)
                        put("reason", b.reason)
                        put("source", b.source)
                        put("riskScore", b.riskScore)
                        put("createdAt", b.createdAt)
                    })
                }
            })
            put("spamSources", JSONArray().apply {
                sources.forEach { s ->
                    put(JSONObject().apply {
                        put("name", s.name)
                        put("urlTemplate", s.urlTemplate)
                        put("category", s.category)
                        put("keywordsCsv", s.keywordsCsv)
                        put("isEnabled", s.isEnabled)
                        put("isBuiltIn", s.isBuiltIn)
                        put("priority", s.priority)
                    })
                }
            })
            put("lookupCache", JSONArray().apply {
                caches.forEach { c ->
                    put(JSONObject().apply {
                        put("normalizedNumber", c.normalizedNumber)
                        put("riskScore", c.riskScore)
                        put("verdict", c.verdict)
                        put("reportCount", c.reportCount)
                        put("summary", c.summary)
                        put("sourceBreakdownJson", c.sourceBreakdownJson)
                        put("fetchedAt", c.fetchedAt)
                    })
                }
            })
        }

        root.toString(2) to Triple(blocked.size, sources.size, caches.size)
    }

    suspend fun saveBackupOnPhone(
        autoBlockEnabled: Boolean,
        spamThreshold: Int,
        blockHiddenNumbers: Boolean
    ): PhoneBackupSummary = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(Date())
        val fileName = "spamcaller_backup_$stamp.json"
        val (jsonString, counts) = createBackupPayloadJson(
            autoBlockEnabled,
            spamThreshold,
            blockHiddenNumbers
        )

        val file = File(backupDir, fileName)
        file.writeText(jsonString, Charsets.UTF_8)

        val record = LocalBackupRecordEntity(
            fileName = fileName,
            filePath = file.absolutePath,
            blockedCount = counts.first,
            sourcesCount = counts.second,
            cacheCount = counts.third,
            sizeBytes = file.length()
        )
        dao.insertBackupRecord(record)

        PhoneBackupSummary(
            fileName = fileName,
            filePath = file.absolutePath,
            blockedCount = counts.first,
            sourcesCount = counts.second,
            cacheCount = counts.third,
            sizeBytes = file.length()
        )
    }

    suspend fun exportBackupToUri(
        uri: Uri,
        autoBlockEnabled: Boolean,
        spamThreshold: Int,
        blockHiddenNumbers: Boolean
    ): PhoneBackupSummary = withContext(Dispatchers.IO) {
        val (jsonString, counts) = createBackupPayloadJson(
            autoBlockEnabled,
            spamThreshold,
            blockHiddenNumbers
        )
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Impossibile scrivere il file di backup sul telefono")

        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "spamcaller_export.json"
        val record = LocalBackupRecordEntity(
            fileName = fileName,
            filePath = uri.toString(),
            blockedCount = counts.first,
            sourcesCount = counts.second,
            cacheCount = counts.third,
            sizeBytes = bytes.size.toLong()
        )
        dao.insertBackupRecord(record)

        PhoneBackupSummary(
            fileName = fileName,
            filePath = uri.toString(),
            blockedCount = counts.first,
            sourcesCount = counts.second,
            cacheCount = counts.third,
            sizeBytes = bytes.size.toLong()
        )
    }

    suspend fun restoreFromLocalFile(filePath: String): Pair<RestoreSummary, Triple<Boolean, Int, Boolean>?> =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            require(file.exists()) { "File backup non trovato sul telefono" }
            restoreFromJsonString(file.readText(Charsets.UTF_8))
        }

    suspend fun restoreFromUri(uri: Uri): Pair<RestoreSummary, Triple<Boolean, Int, Boolean>?> =
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("Impossibile leggere il file selezionato")
            restoreFromJsonString(text)
        }

    suspend fun deleteLocalBackup(record: LocalBackupRecordEntity) = withContext(Dispatchers.IO) {
        val f = File(record.filePath)
        if (f.exists()) f.delete()
        dao.deleteBackupRecord(record.id)
    }

    private suspend fun restoreFromJsonString(
        rawJson: String
    ): Pair<RestoreSummary, Triple<Boolean, Int, Boolean>?> {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "Spamcaller" || root.has("blockedNumbers")) {
            "File JSON non compatibile con Spamcaller"
        }

        val blockedList = mutableListOf<BlockedNumberEntity>()
        val blockedArr = root.optJSONArray("blockedNumbers") ?: JSONArray()
        for (i in 0 until blockedArr.length()) {
            val item = blockedArr.getJSONObject(i)
            val phone = item.optString("phoneNumber").trim()
            if (phone.isNotBlank()) {
                blockedList.add(
                    BlockedNumberEntity(
                        phoneNumber = phone,
                        displayName = item.optString("displayName", "Numero Bloccato"),
                        reason = item.optString("reason", "Ripristinato da backup"),
                        source = item.optString("source", "Backup Locale"),
                        riskScore = item.optInt("riskScore", 90),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }
        if (blockedList.isNotEmpty()) {
            dao.upsertBlockedNumbers(blockedList)
        }

        val existingTemplates = dao.getAllSpamSourcesOnce().map { it.urlTemplate }.toSet()
        val newSources = mutableListOf<SpamSourceEntity>()
        val sourcesArr = root.optJSONArray("spamSources") ?: JSONArray()
        for (i in 0 until sourcesArr.length()) {
            val item = sourcesArr.getJSONObject(i)
            val template = item.optString("urlTemplate").trim()
            val name = item.optString("name").trim()
            if (template.isNotBlank() && name.isNotBlank() && template !in existingTemplates) {
                newSources.add(
                    SpamSourceEntity(
                        name = name,
                        urlTemplate = template,
                        category = item.optString("category", "Sito personalizzato"),
                        keywordsCsv = item.optString("keywordsCsv", "spam,truffa,telemarketing"),
                        isEnabled = item.optBoolean("isEnabled", true),
                        isBuiltIn = item.optBoolean("isBuiltIn", false),
                        priority = item.optInt("priority", 20)
                    )
                )
            }
        }
        if (newSources.isNotEmpty()) {
            dao.insertSpamSources(newSources)
        }

        val cacheList = mutableListOf<SpamLookupCacheEntity>()
        val cacheArr = root.optJSONArray("lookupCache") ?: JSONArray()
        for (i in 0 until cacheArr.length()) {
            val item = cacheArr.getJSONObject(i)
            val num = item.optString("normalizedNumber").trim()
            if (num.isNotBlank()) {
                cacheList.add(
                    SpamLookupCacheEntity(
                        normalizedNumber = num,
                        riskScore = item.optInt("riskScore", 0),
                        verdict = item.optString("verdict", "UNKNOWN"),
                        reportCount = item.optInt("reportCount", 0),
                        summary = item.optString("summary", ""),
                        sourceBreakdownJson = item.optString("sourceBreakdownJson", "[]"),
                        fetchedAt = item.optLong("fetchedAt", System.currentTimeMillis())
                    )
                )
            }
        }
        if (cacheList.isNotEmpty()) {
            dao.upsertLookupCaches(cacheList)
        }

        val settingsObj = root.optJSONObject("settings")
        val settingsTriple = if (settingsObj != null) {
            Triple(
                settingsObj.optBoolean("autoBlockEnabled", true),
                settingsObj.optInt("spamThreshold", 70),
                settingsObj.optBoolean("blockHiddenNumbers", false)
            )
        } else null

        return RestoreSummary(
            blockedRestored = blockedList.size,
            sourcesRestored = newSources.size,
            cacheRestored = cacheList.size
        ) to settingsTriple
    }
}
