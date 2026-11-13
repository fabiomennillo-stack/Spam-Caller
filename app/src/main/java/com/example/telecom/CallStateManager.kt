package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.remote.SpamVerdict
import com.example.ui.incall.InCallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LiveCallStatus(val labelIt: String) {
    RINGING("Chiamata in arrivo…"),
    DIALING("Composizione in corso…"),
    ACTIVE("Chiamata attiva"),
    HOLDING("In attesa"),
    DISCONNECTED("Chiamata terminata")
}

data class ActiveCallUiState(
    val phoneNumber: String,
    val displayName: String,
    val photoUri: String?,
    val contactNotes: String?,
    val spamScore: Int,
    val spamVerdict: SpamVerdict,
    val spamSummary: String,
    val status: LiveCallStatus,
    val connectedAtMillis: Long?,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false
)

object CallStateManager {

    const val INCOMING_CHANNEL_ID = "spamcaller_incoming_calls"
    const val SPAM_ALERT_CHANNEL_ID = "spamcaller_spam_alerts"
    const val INCOMING_NOTIFICATION_ID = 4101

    private var currentCall: Call? = null
    private var inCallServiceRef: SpamcallerInCallService? = null

    private val _activeCallState = MutableStateFlow<ActiveCallUiState?>(null)
    val activeCallState: StateFlow<ActiveCallUiState?> = _activeCallState.asStateFlow()

    fun attachService(service: SpamcallerInCallService) {
        inCallServiceRef = service
    }

    fun detachService() {
        inCallServiceRef = null
    }

    fun updateCall(call: Call?, state: ActiveCallUiState?) {
        currentCall = call
        _activeCallState.value = state
    }

    fun updateMetadata(
        displayName: String,
        photoUri: String?,
        contactNotes: String?,
        spamScore: Int,
        spamVerdict: SpamVerdict,
        spamSummary: String
    ) {
        val current = _activeCallState.value ?: return
        _activeCallState.value = current.copy(
            displayName = displayName,
            photoUri = photoUri,
            contactNotes = contactNotes,
            spamScore = spamScore,
            spamVerdict = spamVerdict,
            spamSummary = spamSummary
        )
    }

    fun answerCall() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun declineOrHangUp() {
        val call = currentCall ?: return
        val status = _activeCallState.value?.status
        if (status == LiveCallStatus.RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    fun toggleMute() {
        val current = _activeCallState.value ?: return
        val next = !current.isMuted
        inCallServiceRef?.setMuted(next)
        _activeCallState.value = current.copy(isMuted = next)
    }

    fun toggleSpeaker() {
        val current = _activeCallState.value ?: return
        val next = !current.isSpeakerOn
        inCallServiceRef?.setSpeakerRoute(next)
        _activeCallState.value = current.copy(isSpeakerOn = next)
    }

    fun toggleHold() {
        val call = currentCall ?: return
        val current = _activeCallState.value ?: return
        if (current.isOnHold) {
            call.unhold()
            _activeCallState.value = current.copy(isOnHold = false, status = LiveCallStatus.ACTIVE)
        } else {
            call.hold()
            _activeCallState.value = current.copy(isOnHold = true, status = LiveCallStatus.HOLDING)
        }
    }

    fun sendDtmf(digit: Char) {
        currentCall?.playDtmfTone(digit)
        currentCall?.stopDtmfTone()
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incomingChannel = NotificationChannel(
            INCOMING_CHANNEL_ID,
            context.getString(R.string.incoming_call_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(ringtoneUri, audioAttributes)
            enableVibration(true)
        }
        manager.createNotificationChannel(incomingChannel)

        val spamChannel = NotificationChannel(
            SPAM_ALERT_CHANNEL_ID,
            context.getString(R.string.spam_alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(spamChannel)
    }

    fun showIncomingCallNotification(
        context: Context,
        title: String,
        subtitle: String
    ) {
        ensureNotificationChannels(context)
        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, INCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            manager?.notify(INCOMING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelIncomingCallNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(INCOMING_NOTIFICATION_ID)
    }

    fun showSpamBlockedNotification(context: Context, number: String, reason: String) {
        ensureNotificationChannels(context)
        val notification = NotificationCompat.Builder(context, SPAM_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Chiamata Spam Bloccata: $number")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            manager?.notify(number.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }
}
