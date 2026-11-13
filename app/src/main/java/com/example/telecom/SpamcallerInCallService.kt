package com.example.telecom

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.data.device.GoogleContactsRepository
import com.example.data.local.SpamcallerDatabase
import com.example.data.remote.OnlineSpamLookupService
import com.example.data.remote.SpamVerdict
import com.example.ui.incall.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpamcallerInCallService : InCallService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observedCall: Call? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            syncCallState(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallStateManager.attachService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        observedCall?.unregisterCallback(callCallback)
        observedCall = call
        call.registerCallback(callCallback)

        val state = extractCallState(call)
        syncCallState(call, state)

        val number = call.details?.handle?.schemeSpecificPart.orEmpty().ifBlank { "Numero Sconosciuto" }
        if (state == Call.STATE_RINGING) {
            CallStateManager.showIncomingCallNotification(
                context = applicationContext,
                title = "Chiamata in arrivo",
                subtitle = number
            )
        }

        val intent = Intent(applicationContext, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)

        resolveCallerIdentity(number, state == Call.STATE_RINGING)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (observedCall == call) {
            observedCall = null
        }
        CallStateManager.cancelIncomingCallNotification(applicationContext)
        CallStateManager.updateCall(null, null)
    }

    fun setSpeakerRoute(enableSpeaker: Boolean) {
        val route = if (enableSpeaker) {
            CallAudioState.ROUTE_SPEAKER
        } else {
            CallAudioState.ROUTE_WIRED_OR_EARPIECE
        }
        @Suppress("DEPRECATION")
        setAudioRoute(route)
    }

    private fun resolveCallerIdentity(number: String, isRinging: Boolean) {
        serviceScope.launch {
            val contactsRepo = GoogleContactsRepository(applicationContext)
            val contact = contactsRepo.findContactByNumber(number)

            if (contact != null) {
                CallStateManager.updateMetadata(
                    displayName = contact.displayName,
                    photoUri = contact.photoUri,
                    contactNotes = contact.notes,
                    spamScore = 0,
                    spamVerdict = SpamVerdict.SAFE,
                    spamSummary = contact.googleAccountName?.let { "Contatto Google ($it)" }
                        ?: "Contatto in rubrica"
                )
                if (isRinging) {
                    CallStateManager.showIncomingCallNotification(
                        context = applicationContext,
                        title = contact.displayName,
                        subtitle = number
                    )
                }
            } else {
                val dao = SpamcallerDatabase.getInstance(applicationContext).spamcallerDao()
                val report = runCatching {
                    OnlineSpamLookupService(dao).lookupNumber(number, forceRefresh = false)
                }.getOrNull()

                if (report != null) {
                    val title = when (report.verdict) {
                        SpamVerdict.HIGH_RISK -> "ATTENZIONE SPAM (${report.riskScore}/100)"
                        SpamVerdict.SUSPICIOUS -> "Numero Sospetto (${report.riskScore}/100)"
                        else -> number
                    }
                    CallStateManager.updateMetadata(
                        displayName = if (report.verdict == SpamVerdict.HIGH_RISK) "Possibile Spam" else number,
                        photoUri = null,
                        contactNotes = null,
                        spamScore = report.riskScore,
                        spamVerdict = report.verdict,
                        spamSummary = report.summary
                    )
                    if (isRinging) {
                        CallStateManager.showIncomingCallNotification(
                            context = applicationContext,
                            title = title,
                            subtitle = "$number • ${report.summary}"
                        )
                    }
                }
            }
        }
    }

    private fun syncCallState(call: Call, state: Int) {
        val number = call.details?.handle?.schemeSpecificPart.orEmpty().ifBlank { "Numero Sconosciuto" }
        val existing = CallStateManager.activeCallState.value
        val mappedStatus = when (state) {
            Call.STATE_RINGING -> LiveCallStatus.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> LiveCallStatus.DIALING
            Call.STATE_HOLDING -> LiveCallStatus.HOLDING
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> LiveCallStatus.DISCONNECTED
            else -> LiveCallStatus.ACTIVE
        }

        if (mappedStatus != LiveCallStatus.RINGING) {
            CallStateManager.cancelIncomingCallNotification(applicationContext)
        }

        val connectedAt = if (mappedStatus == LiveCallStatus.ACTIVE) {
            existing?.connectedAtMillis ?: System.currentTimeMillis()
        } else {
            existing?.connectedAtMillis
        }

        CallStateManager.updateCall(
            call,
            ActiveCallUiState(
                phoneNumber = number,
                displayName = existing?.displayName ?: number,
                photoUri = existing?.photoUri,
                contactNotes = existing?.contactNotes,
                spamScore = existing?.spamScore ?: 0,
                spamVerdict = existing?.spamVerdict ?: SpamVerdict.UNKNOWN,
                spamSummary = existing?.spamSummary ?: "Verifica ID chiamante in corso…",
                status = mappedStatus,
                connectedAtMillis = connectedAt,
                isMuted = existing?.isMuted ?: false,
                isSpeakerOn = existing?.isSpeakerOn ?: false,
                isOnHold = mappedStatus == LiveCallStatus.HOLDING
            )
        )
    }

    private fun extractCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallStateManager.detachService()
        serviceScope.cancel()
    }
}
