package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.device.SystemRoleStatus
import com.example.data.local.LocalBackupRecordEntity
import com.example.data.remote.UserProfile
import com.example.ui.components.BrandedTopHeader
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSystemScreen(
    firebaseUser: FirebaseUser?,
    cloudProfile: UserProfile?,
    roleStatus: SystemRoleStatus,
    backupRecords: List<LocalBackupRecordEntity>,
    onRequestDefaultDialerRole: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onCreateBackupNow: () -> Unit,
    onExportBackupToPhoneFolder: () -> Unit,
    onRestoreBackupFromFile: () -> Unit,
    onRestoreRecord: (LocalBackupRecordEntity) -> Unit,
    onDeleteRecord: (LocalBackupRecordEntity) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val gradients = LocalSpamcallerGradients.current

    val userName = cloudProfile?.displayName?.ifBlank { null }
        ?: firebaseUser?.displayName
        ?: "Utente Google"
    val userEmail = cloudProfile?.email?.ifBlank { null }
        ?: firebaseUser?.email
        ?: ""
    val userPhoto = cloudProfile?.photoUrl?.ifBlank { null }
        ?: firebaseUser?.photoUrl?.toString()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandedTopHeader(
                title = "Backup & Sistema",
                subtitle = "Account Google, ruolo sistema e salvataggi locali"
            )
        }

        // Google Account Card
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(gradients.heroBlackBlue, RoundedCornerShape(22.dp))
                    .border(1.dp, gradients.accentBorderBrush, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ContactOrCallerAvatar(
                        name = userName,
                        photoUri = userPhoto,
                        size = 54.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBFDBFE),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Accesso Google attivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeEmerald
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Disconnetti account Google",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // System App Decision Card (Default Dialer + Call Screening Role)
        item {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Applicazione di Sistema (Scelta tua)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Decidi se usare Spamcaller al posto dell'app Telefono di sistema oppure attivarla solo per identificare e bloccare le chiamate spam.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SystemRoleActionRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "App Telefono Predefinita",
                        desc = if (roleStatus.isDefaultDialer) {
                            "Attiva: Spamcaller gestisce chiamate in entrata e uscita."
                        } else {
                            "Non attiva: usa l'app Telefono originale o scegli Spamcaller."
                        },
                        isActive = roleStatus.isDefaultDialer,
                        enabled = roleStatus.canRequestDialerRole,
                        buttonTag = "request_default_dialer_btn",
                        onClick = onRequestDefaultDialerRole
                    )

                    SystemRoleActionRow(
                        icon = Icons.Default.Security,
                        title = "App ID Chiamante e Spam",
                        desc = if (roleStatus.isCallScreeningApp) {
                            "Attiva: analizza e blocca automaticamente le chiamate spam dai siti online."
                        } else {
                            "Attiva per filtrare i numeri sconosciuti senza cambiare dialer."
                        },
                        isActive = roleStatus.isCallScreeningApp,
                        enabled = roleStatus.canRequestCallScreeningRole,
                        buttonTag = "request_call_screening_btn",
                        onClick = onRequestCallScreeningRole
                    )
                }
            }
        }

        // Theme Indicator (Follows system theme + Black/Blue Gradients)
        item {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessAuto,
                        contentDescription = null,
                        tint = ElectricAzure
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sfondo Automatico di Sistema",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (gradients.isDark) {
                                "Modalità scura di sistema rilevata • Sfumature Nero & Blu"
                            } else {
                                "Modalità chiara di sistema rilevata • Accenti Nero & Blu"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Phone Local Backup Controls
        item {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Backup Salvati sul Telefono",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Salva numeri bloccati, siti web configurati (Tellows, ChiMiChiama e personalizzati) e cache direttamente nella memoria del tuo telefono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onCreateBackupNow,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_phone_backup_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salva Backup sul Telefono", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportBackupToPhoneFolder,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_backup_saf_btn")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Esporta JSON", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onRestoreBackupFromFile,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_backup_saf_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ripristina File", maxLines = 1)
                        }
                    }
                }
            }
        }

        if (backupRecords.isNotEmpty()) {
            item {
                Text(
                    text = "Archivio Backup Locali (${backupRecords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            items(backupRecords, key = { it.id }) { record ->
                val formattedSize = remember(record.sizeBytes) {
                    Formatter.formatShortFileSize(context, record.sizeBytes)
                }
                val formattedTime = remember(record.createdAt) {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALY)
                        .format(Date(record.createdAt))
                }
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$formattedTime • $formattedSize",
                                style = MaterialTheme.typography.labelSmall,
                                color = LuminousCyan
                            )
                            Text(
                                text = "${record.blockedCount} bloccati • ${record.sourcesCount} siti • ${record.cacheCount} schede",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!record.filePath.startsWith("content://")) {
                            IconButton(onClick = { onRestoreRecord(record) }) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Ripristina ${record.fileName}",
                                    tint = SafeEmerald
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteRecord(record) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina ${record.fileName}",
                                tint = SpamCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemRoleActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    isActive: Boolean,
    enabled: Boolean,
    buttonTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) SafeEmerald else ElectricAzure,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Attiva",
                    tint = SafeEmerald
                )
            } else {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag(buttonTag)
                ) {
                    Text("Attiva")
                }
            }
        }
    }
}
