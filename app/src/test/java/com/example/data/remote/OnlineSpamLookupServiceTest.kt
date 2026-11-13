package com.example.data.remote

import com.example.data.local.SpamSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSpamLookupServiceTest {

    @Test
    fun normalizePhoneNumber_stripsFormattingAndNormalizesPrefix() {
        assertEquals("+393331234567", OnlineSpamLookupService.normalizePhoneNumber("+39 333 123-4567"))
        assertEquals("+393331234567", OnlineSpamLookupService.normalizePhoneNumber("0039 (333) 123 4567"))
        assertEquals("0212345678", OnlineSpamLookupService.normalizePhoneNumber("02 1234 5678"))
    }

    @Test
    fun formatNumberForWebQuery_stripsItalianCountryCode() {
        assertEquals("3331234567", OnlineSpamLookupService.formatNumberForWebQuery("+393331234567"))
        assertEquals("0212345678", OnlineSpamLookupService.formatNumberForWebQuery("0212345678"))
    }

    @Test
    fun parseHtmlSignals_detectsTellowsScoreAndSpamKeywords() {
        val dummyDao = object : com.example.data.local.SpamcallerDao {
            override fun observeBlockedNumbers() = throw NotImplementedError()
            override suspend fun getAllBlockedNumbersOnce() = emptyList<com.example.data.local.BlockedNumberEntity>()
            override suspend fun getBlockedNumber(normalized: String) = null
            override suspend fun upsertBlockedNumber(entity: com.example.data.local.BlockedNumberEntity) {}
            override suspend fun upsertBlockedNumbers(entities: List<com.example.data.local.BlockedNumberEntity>) {}
            override suspend fun deleteBlockedNumber(normalized: String) {}
            override fun observeSpamSources() = throw NotImplementedError()
            override suspend fun getEnabledSpamSourcesOnce() = emptyList<SpamSourceEntity>()
            override suspend fun getAllSpamSourcesOnce() = emptyList<SpamSourceEntity>()
            override suspend fun countSpamSources() = 0
            override suspend fun insertSpamSource(entity: SpamSourceEntity) = 0L
            override suspend fun insertSpamSources(entities: List<SpamSourceEntity>) {}
            override suspend fun updateSpamSource(entity: SpamSourceEntity) {}
            override suspend fun deleteCustomSpamSource(id: Long) {}
            override fun observeLookupCache() = throw NotImplementedError()
            override suspend fun getAllLookupCacheOnce() = emptyList<com.example.data.local.SpamLookupCacheEntity>()
            override suspend fun getLookupCache(normalized: String) = null
            override suspend fun upsertLookupCache(entity: com.example.data.local.SpamLookupCacheEntity) {}
            override suspend fun upsertLookupCaches(entities: List<com.example.data.local.SpamLookupCacheEntity>) {}
            override fun observeBackupRecords() = throw NotImplementedError()
            override suspend fun insertBackupRecord(entity: com.example.data.local.LocalBackupRecordEntity) = 0L
            override suspend fun deleteBackupRecord(id: Long) {}
        }

        val service = OnlineSpamLookupService(dummyDao)
        val source = SpamSourceEntity(
            name = "Tellows Italia",
            urlTemplate = "https://www.tellows.it/num/{number}",
            category = "Community"
        )
        val html = """
            <html><body>
            <div>Tellows Score: 8 / 9</div>
            <p>18 segnalazioni ricevute: Call center aggressivo e telemarketing luce e gas.</p>
            </body></html>
        """.trimIndent()

        val result = service.parseHtmlSignals(
            source = source,
            url = "https://www.tellows.it/num/0212345678",
            httpCode = 200,
            rawHtml = html,
            latency = 120L
        )

        assertTrue(result.reachable)
        assertTrue(result.detectedScore >= 80)
        assertEquals(18, result.reportsFound)
        assertTrue(result.matchedTags.contains("telemarketing"))
    }
}
