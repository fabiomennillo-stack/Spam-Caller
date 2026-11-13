package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.device.SystemRoleStatus
import com.example.data.local.LocalBackupRecordEntity
import com.example.data.remote.UserProfile
import com.example.ui.components.BrandedTopHeader
import com.example.ui.components.ContactColorPalette
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.example.ui.theme.SuspectAmber
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSystemScreen(
    firebaseUser: FirebaseUser?,
    cloudProfile: UserProfile?,
    roleStatus: SystemRoleStatus,
    backupRecords: List<LocalBackupRecordEntity>,
    contactNameColorArgb: Long,
    onUpdateContactNameColor: (Long) -> Unit,
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
                title = "Impostazioni & Backup",
                subtitle = "Colori contatti, ruolo sistema e salvataggi locali"
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

        // Contact Name Color Customization Card (Impostazioni Colori Nomi Contatti)
        item {
            ContactColorSettingsCard(
                currentArgb = contactNameColorArgb,
                onSelectColor = onUpdateContactNameColor
            )
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
                        text = "Salva numeri bloccati, siti web configurati (Tellows, ChiMiChiama e personalizzati), preferiti, colore contatti e cache direttamente nella memoria del tuo telefono.",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactColorSettingsCard(
    currentArgb: Long,
    onSelectColor: (Long) -> Unit
) {
    val previewColor = ContactColorPalette.resolveNameColor(
        argb = currentArgb,
        defaultColor = MaterialTheme.colorScheme.onSurface
    )

    var showCustomSliders by remember { mutableStateOf(false) }
    val baseColor = if (currentArgb == 0L) ElectricAzure else Color(currentArgb.toInt())
    var red by remember { mutableFloatStateOf(baseColor.red * 255f) }
    var green by remember { mutableFloatStateOf(baseColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(baseColor.blue * 255f) }

    LaunchedEffect(currentArgb) {
        if (currentArgb != 0L) {
            val c = Color(currentArgb.toInt())
            red = c.red * 255f
            green = c.green * 255f
            blue = c.blue * 255f
        }
    }

    val sliderColor = Color(
        red = red.roundToInt().coerceIn(0, 255),
        green = green.roundToInt().coerceIn(0, 255),
        blue = blue.roundToInt().coerceIn(0, 255)
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .testTag("contact_color_settings_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = ElectricAzure
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Colore Nomi Contatti",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = { showCustomSliders = !showCustomSliders },
                    modifier = Modifier.testTag("toggle_custom_rgb_btn")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showCustomSliders) "Chiudi RGB" else "RGB")
                }
            }

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactOrCallerAvatar(name = "Mario Rossi", photoUri = null, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mario Rossi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = previewColor,
                            modifier = Modifier.testTag("contact_color_preview_name")
                        )
                        Text(
                            text = "Anteprima colore nome in rubrica • +39 347 123 4567",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = SuspectAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Preset Color Swatches
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ContactColorPalette.presets.forEach { preset ->
                    val isSelected = currentArgb == preset.argb
                    val swatchColor = if (preset.argb == 0L) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color(preset.argb.toInt())
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectColor(preset.argb) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("color_preset_${preset.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) ElectricAzure else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "${preset.label} selezionato",
                                    tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (preset.argb == 0L) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            // Expandable Custom RGB Color Picker
            AnimatedVisibility(visible = showCustomSliders) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Colore personalizzato (RGB)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    RgbSliderRow("Rosso (R)", red, Color(0xFFEF4444)) { red = it }
                    RgbSliderRow("Verde (G)", green, Color(0xFF10B981)) { green = it }
                    RgbSliderRow("Blu (B)", blue, Color(0xFF3B82F6)) { blue = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectColor(0L) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Automatico")
                        }
                        Button(
                            onClick = {
                                onSelectColor(ContactColorPalette.toArgbLong(sliderColor))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = sliderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("apply_custom_rgb_btn")
                        ) {
                            Text(
                                text = "Applica Colore",
                                color = if (sliderColor.luminance() > 0.5f) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RgbSliderRow(
    label: String,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${value.roundToInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            )
        )
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
