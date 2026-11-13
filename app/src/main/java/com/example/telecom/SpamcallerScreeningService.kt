package com.example.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.local.SpamcallerDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.OnlineSpamLookupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SpamcallerScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        serviceScope.launch {
            val db = SpamcallerDatabase.getInstance(applicationContext)
            val dao = db.spamcallerDao()
            val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
            val lookupService = OnlineSpamLookupService(dao)

            if (rawNumber.isBlank() && prefs.blockHiddenNumbers) {
                val blockResponse = buildBlockResponse()
                respondToCall(callDetails, blockResponse)
                CallStateManager.showSpamBlockedNotification(
                    applicationContext,
                    "Numero Privato",
                    "Bloccato automaticamente da Spamcaller"
                )
                return@launch
            }

            val normalized = OnlineSpamLookupService.normalizePhoneNumber(rawNumber)
            val manuallyBlocked = dao.getBlockedNumber(normalized)
            if (manuallyBlocked != null) {
                respondToCall(callDetails, buildBlockResponse())
                CallStateManager.showSpamBlockedNotification(
                    applicationContext,
                    rawNumber,
                    manuallyBlocked.reason
                )
                return@launch
            }

            val report = withTimeoutOrNull(2600L) {
                lookupService.lookupNumber(normalized, forceRefresh = false)
            }

            val shouldBlock = prefs.autoBlockEnabled &&
                report != null &&
                report.riskScore >= prefs.spamThreshold

            if (shouldBlock && report != null) {
                respondToCall(callDetails, buildBlockResponse())
                CallStateManager.showSpamBlockedNotification(
                    applicationContext,
                    rawNumber,
                    "Punteggio Spam ${report.riskScore}/100 (${report.summary})"
                )
            } else {
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun buildBlockResponse(): CallResponse {
        val builder = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setSilenceCall(true)
        }
        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
