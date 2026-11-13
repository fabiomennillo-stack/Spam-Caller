package com.example.data.device

import android.content.Context
import android.provider.CallLog
import com.example.data.local.SpamcallerDao
import com.example.data.remote.OnlineSpamLookupService
import com.example.data.remote.SpamVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CallDirection(val labelIt: String) {
    INCOMING("In entrata"),
    OUTGOING("In uscita"),
    MISSED("Persa"),
    BLOCKED("Bloccata"),
    REJECTED("Rifiutata")
}

data class DeviceCallLogItem(
    val id: Long,
    val number: String,
    val normalizedNumber: String,
    val displayName: String,
    val photoUri: String?,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
    val spamScore: Int,
    val spamVerdict: SpamVerdict,
    val isBlocked: Boolean,
    val contactNotes: String?
)

class DeviceCallLogRepository(
    private val context: Context,
    private val dao: SpamcallerDao
) {
    suspend fun loadRecentCalls(
        contacts: List<DeviceContact>,
        limit: Int = 80
    ): List<DeviceCallLogItem> = withContext(Dispatchers.IO) {
        val blockedSet = dao.getAllBlockedNumbersOnce().associateBy { it.phoneNumber }
        val cacheMap = dao.getAllLookupCacheOnce().associateBy { it.normalizedNumber }

        val items = mutableListOf<DeviceCallLogItem>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                while (cursor.moveToNext() && items.size < limit) {
                    val rawNumber = cursor.getString(numIdx).orEmpty().ifBlank { "Numero Privato" }
                    val normalized = OnlineSpamLookupService.normalizePhoneNumber(rawNumber)
                    val cachedName = cursor.getString(nameIdx)
                    val matchedContact = findMatchingContact(contacts, normalized)

                    val blockedEntry = blockedSet[normalized]
                    val cachedSpam = cacheMap[normalized]

                    val type = cursor.getInt(typeIdx)
                    val direction = when (type) {
                        CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                        CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
                        CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                        else -> CallDirection.INCOMING
                    }

                    val verdict = when {
                        blockedEntry != null -> SpamVerdict.HIGH_RISK
                        cachedSpam != null -> runCatching { SpamVerdict.valueOf(cachedSpam.verdict) }
                            .getOrDefault(SpamVerdict.UNKNOWN)
                        else -> SpamVerdict.UNKNOWN
                    }

                    items.add(
                        DeviceCallLogItem(
                            id = cursor.getLong(idIdx),
                            number = rawNumber,
                            normalizedNumber = normalized,
                            displayName = matchedContact?.displayName
                                ?: cachedName?.takeIf { it.isNotBlank() }
                                ?: blockedEntry?.displayName
                                ?: rawNumber,
                            photoUri = matchedContact?.photoUri,
                            direction = direction,
                            timestamp = cursor.getLong(dateIdx),
                            durationSeconds = cursor.getLong(durIdx),
                            spamScore = when {
                                blockedEntry != null -> blockedEntry.riskScore
                                cachedSpam != null -> cachedSpam.riskScore
                                else -> 0
                            },
                            spamVerdict = verdict,
                            isBlocked = blockedEntry != null || direction == CallDirection.BLOCKED,
                            contactNotes = matchedContact?.notes
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }

        items
    }

    private fun findMatchingContact(
        contacts: List<DeviceContact>,
        normalized: String
    ): DeviceContact? {
        val suffix = normalized.takeLast(8)
        return contacts.firstOrNull { contact ->
            contact.phoneNumbers.any { phone ->
                phone.normalizedNumber == normalized ||
                    (suffix.length >= 7 && phone.normalizedNumber.endsWith(suffix))
            }
        }
    }
}
