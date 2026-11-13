package com.example.ui.incall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.SpamVerdict
import com.example.telecom.ActiveCallUiState
import com.example.telecom.CallStateManager
import com.example.telecom.LiveCallStatus
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.components.SpamVerdictBadge
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.example.ui.theme.SpamcallerTheme
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpamcallerTheme {
                val activeCall by CallStateManager.activeCallState.collectAsStateWithLifecycle()
                LaunchedEffect(activeCall) {
                    if (activeCall == null || activeCall?.status == LiveCallStatus.DISCONNECTED) {
                        delay(400L)
                        finish()
                    }
                }
                val state = activeCall
                if (state != null) {
                    InCallScreenContent(state = state)
                }
            }
        }
    }
}

@Composable
private fun InCallScreenContent(state: ActiveCallUiState) {
    val gradients = LocalSpamcallerGradients.current
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.status, state.connectedAtMillis) {
        while (state.status == LiveCallStatus.ACTIVE && state.connectedAtMillis != null) {
            elapsedSeconds = ((System.currentTimeMillis() - state.connectedAtMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val isSpam = state.spamVerdict == SpamVerdict.HIGH_RISK
    val timerText = if (state.status == LiveCallStatus.ACTIVE) {
        String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    } else {
        state.status.labelIt
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.heroBlackBlue)
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top caller identity + online spam verdict
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = LuminousCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactOrCallerAvatar(
                    name = state.displayName,
                    photoUri = state.photoUri,
                    isSpam = isSpam,
                    size = 112.dp
                )
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = state.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBFDBFE)
                )
                SpamVerdictBadge(verdict = state.spamVerdict, score = state.spamScore)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = state.spamSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!state.contactNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${state.contactNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LuminousCyan,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom call controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                if (state.status == LiveCallStatus.RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilledIconButton(
                            onClick = { CallStateManager.declineOrHangUp() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SpamCrimson),
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Rifiuta", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        FilledIconButton(
                            onClick = { CallStateManager.answerCall() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeEmerald),
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Rispondi", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilledIconButton(
                            onClick = { CallStateManager.toggleMute() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isMuted) ElectricAzure else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.Default.MicOff, contentDescription = "Muto", tint = Color.White)
                        }
                        FilledIconButton(
                            onClick = { CallStateManager.toggleSpeaker() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isSpeakerOn) ElectricAzure else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Vivavoce", tint = Color.White)
                        }
                        FilledIconButton(
                            onClick = { CallStateManager.toggleHold() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isOnHold) ElectricAzure else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Attesa", tint = Color.White)
                        }
                    }

                    FilledIconButton(
                        onClick = { CallStateManager.declineOrHangUp() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SpamCrimson),
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Termina chiamata", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
