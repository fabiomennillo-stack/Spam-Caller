package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.ScreeningPreferences
import com.example.data.local.SpamSourceEntity
import com.example.ui.components.BrandedTopHeader
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import kotlin.math.roundToInt

@Composable
fun ProtectionScreen(
    spamSources: List<SpamSourceEntity>,
    blockedNumbers: List<BlockedNumberEntity>,
    cachedCount: Int,
    preferences: ScreeningPreferences,
    onToggleSource: (SpamSourceEntity, Boolean) -> Unit,
    onAddCustomSource: (String, String, String) -> Unit,
    onDeleteCustomSource: (SpamSourceEntity) -> Unit,
    onUpdateSettings: (Boolean, Int, Boolean) -> Unit,
    onBlockManualNumber: (String, String, String, Int) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onTestLookupOnline: (String) -> Unit
) {
    val gradients = LocalSpamcallerGradients.current
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var testNumber by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandedTopHeader(
                title = "Protezione & Siti",
                subtitle = "Fonti online anti-spam e lista numeri bloccati"
            )
        }

        // Hero Shield Status Card
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(gradients.heroBlackBlue, RoundedCornerShape(24.dp))
                    .border(1.dp, gradients.accentBorderBrush, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Scudo Anti-Spam",
                            tint = if (preferences.autoBlockEnabled) SafeEmerald else LuminousCyan,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (preferences.autoBlockEnabled) "Scudo Anti-Spam Attivo" else "Solo Identificazione",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${spamSources.count { it.isEnabled }} siti attivi • ${blockedNumbers.size} bloccati • $cachedCount schede in cache",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBFDBFE)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Blocco automatico chiamate spam",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Switch(
                            checked = preferences.autoBlockEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(
                                    enabled,
                                    preferences.spamThreshold,
                                    preferences.blockHiddenNumbers
                                )
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricAzure),
                            modifier = Modifier.testTag("auto_block_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Blocca numeri privati / anonimi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Switch(
                            checked = preferences.blockHiddenNumbers,
                            onCheckedChange = { hidden ->
                                onUpdateSettings(
                                    preferences.autoBlockEnabled,
                                    preferences.spamThreshold,
                                    hidden
                                )
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricAzure),
                            modifier = Modifier.testTag("block_hidden_switch")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Soglia punteggio blocco",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFDBEAFE)
                            )
                            Text(
                                text = "${preferences.spamThreshold}/100",
                                style = MaterialTheme.typography.labelLarge,
                                color = LuminousCyan
                            )
                        }
                        Slider(
                            value = preferences.spamThreshold.toFloat(),
                            onValueChange = { value ->
                                onUpdateSettings(
                                    preferences.autoBlockEnabled,
                                    value.roundToInt(),
                                    preferences.blockHiddenNumbers
                                )
                            },
                            valueRange = 30f..95f,
                            colors = SliderDefaults.colors(
                                thumbColor = LuminousCyan,
                                activeTrackColor = ElectricAzure
                            )
                        )
                    }
                }
            }
        }

        // Quick Test Lookup Bar
        item {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = testNumber,
                        onValueChange = { testNumber = it },
                        placeholder = { Text("Prova numero sui siti online…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { if (testNumber.isNotBlank()) onTestLookupOnline(testNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = "Verifica")
                    }
                }
            }
        }

        // Online Spam Websites Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Siti Online Consultati",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tellows, Chi Mi Chiama, ChiChiama, Segnalazioni Numeri, Numeri Spam + tuoi siti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showAddSiteDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_custom_site_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi")
                }
            }
        }

        items(spamSources, key = { it.id }) { source ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                ElectricAzure.copy(alpha = 0.14f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = source.name,
                            tint = LuminousCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = source.urlTemplate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = source.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricAzure
                        )
                    }
                    if (!source.isBuiltIn) {
                        IconButton(onClick = { onDeleteCustomSource(source) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina ${source.name}",
                                tint = SpamCrimson
                            )
                        }
                    }
                    Switch(
                        checked = source.isEnabled,
                        onCheckedChange = { enabled -> onToggleSource(source, enabled) }
                    )
                }
            }
        }

        // Blocked Numbers Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lista Nera (${blockedNumbers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { showAddBlockDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_blocked_number_btn")
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Blocca")
                }
            }
        }

        if (blockedNumbers.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "Nessun numero bloccato manualmente. I numeri segnalati come spam ad alto rischio verranno bloccati automaticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(blockedNumbers, key = { it.phoneNumber }) { blocked ->
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
                            imageVector = Icons.Default.Block,
                            contentDescription = "Bloccato",
                            tint = SpamCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = blocked.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${blocked.phoneNumber} • ${blocked.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onUnblockNumber(blocked.phoneNumber) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sblocca ${blocked.phoneNumber}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSiteDialog) {
        AddCustomSiteDialog(
            onDismiss = { showAddSiteDialog = false },
            onConfirm = { name, url, keywords ->
                onAddCustomSource(name, url, keywords)
                showAddSiteDialog = false
            }
        )
    }

    if (showAddBlockDialog) {
        AddBlockedNumberDialog(
            onDismiss = { showAddBlockDialog = false },
            onConfirm = { number, label, reason ->
                onBlockManualNumber(number, label, reason, 100)
                showAddBlockDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomSiteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var keywords by remember { mutableStateOf("spam,truffa,telemarketing,call center") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Sito Anti-Spam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Inserisci {number} nel punto dell'URL dove va il numero di telefono.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome sito (es. PagineGialle / Altro)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (es. https://sito.it/num/{number})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Parole chiave spam (separate da virgola)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, url, keywords) }) {
                Text("Salva Sito")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun AddBlockedNumberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Telemarketing indesiderato") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blocca Numero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Numero di telefono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Etichetta (opzionale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(number, label.ifBlank { number }, reason) },
                colors = ButtonDefaults.buttonColors(containerColor = SpamCrimson)
            ) {
                Text("Blocca")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
