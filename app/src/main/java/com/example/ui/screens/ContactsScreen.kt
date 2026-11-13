package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.device.DeviceContact
import com.example.ui.components.BrandedTopHeader
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<DeviceContact>,
    googleAccounts: List<String>,
    lastSyncMillis: Long,
    isSyncing: Boolean,
    hasContactsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onSyncGoogleContacts: () -> Unit,
    onCallNumber: (String) -> Unit,
    onBlockNumber: (String, String, String, Int) -> Unit
) {
    val gradients = LocalSpamcallerGradients.current
    var query by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<DeviceContact?>(null) }

    val filtered = remember(contacts, query) {
        val q = query.trim().lowercase(Locale.ITALIAN)
        if (q.isBlank()) contacts
        else contacts.filter { c ->
            c.displayName.lowercase(Locale.ITALIAN).contains(q) ||
                c.phoneNumbers.any { it.number.contains(q) || it.normalizedNumber.contains(q) } ||
                (c.notes?.lowercase(Locale.ITALIAN)?.contains(q) == true) ||
                (c.organization?.lowercase(Locale.ITALIAN)?.contains(q) == true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BrandedTopHeader(
                title = "Contatti Google",
                subtitle = "${contacts.size} contatti con foto e notizie salvate"
            )
        }

        // Google Sync Status Card
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(gradients.heroBlackBlue, RoundedCornerShape(22.dp))
                    .border(1.dp, gradients.accentBorderBrush, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronizzazione Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (googleAccounts.isNotEmpty()) {
                                    "Account: ${googleAccounts.joinToString(", ")}"
                                } else {
                                    "Rubrica sincronizzata con il dispositivo"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBFDBFE)
                            )
                        }
                        Button(
                            onClick = onSyncGoogleContacts,
                            enabled = !isSyncing && hasContactsPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sync_google_contacts_btn")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizza")
                        }
                    }
                    if (lastSyncMillis > 0L) {
                        val dateText = SimpleDateFormat("dd MMM HH:mm", Locale.ITALY)
                            .format(Date(lastSyncMillis))
                        Text(
                            text = "Ultimo aggiornamento: $dateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousCyan
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cerca per nome, numero, azienda o note…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Pulisci ricerca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("contacts_search_input")
            )
        }

        if (!hasContactsPermission) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Permesso Contatti richiesto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Concedi l'accesso ai Contatti per mostrare i tuoi contatti Google con le foto profilo e tutte le note/notizie salvate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_contacts_permission_btn")
                        ) {
                            Text("Abilita Contatti Google")
                        }
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = ElectricAzure,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Nessun contatto trovato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tocca Sincronizza per importare i contatti salvati sul tuo account Google.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.contactId }) { contact ->
                ContactRowCard(
                    contact = contact,
                    onClick = { selectedContact = contact },
                    onCallClick = {
                        contact.phoneNumbers.firstOrNull()?.number?.let(onCallNumber)
                    }
                )
            }
        }
    }

    val contactToShow = selectedContact
    if (contactToShow != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedContact = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ContactFullDetailSheet(
                contact = contactToShow,
                onCall = { num ->
                    selectedContact = null
                    onCallNumber(num)
                },
                onBlock = { num ->
                    onBlockNumber(num, contactToShow.displayName, "Bloccato da rubrica", 95)
                    selectedContact = null
                }
            )
        }
    }
}

@Composable
private fun ContactRowCard(
    contact: DeviceContact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val primaryNumber = contact.phoneNumbers.firstOrNull()?.number.orEmpty()
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactOrCallerAvatar(
                name = contact.displayName,
                photoUri = contact.photoUri,
                size = 52.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!contact.googleAccountName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricAzure.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Google",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricAzure,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (primaryNumber.isNotBlank()) {
                    Text(
                        text = primaryNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!contact.notes.isNullOrBlank()) {
                    Text(
                        text = "Notizie: ${contact.notes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LuminousCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (primaryNumber.isNotBlank()) {
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Chiama ${contact.displayName}",
                        tint = SafeEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactFullDetailSheet(
    contact: DeviceContact,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactOrCallerAvatar(
                name = contact.displayName,
                photoUri = contact.photoUri,
                size = 72.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!contact.organization.isNullOrBlank()) {
                    Text(
                        text = contact.organization,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!contact.googleAccountName.isNullOrBlank()) {
                    Text(
                        text = "Sincronizzato: ${contact.googleAccountName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricAzure
                    )
                }
            }
        }

        HorizontalDivider()

        contact.phoneNumbers.forEach { phone ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(phone.number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(phone.typeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onBlock(phone.number) }) {
                        Icon(Icons.Default.Block, contentDescription = "Blocca", tint = SpamCrimson, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = { onCall(phone.number) },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeEmerald)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chiama")
                    }
                }
            }
        }

        if (contact.emails.isNotEmpty()) {
            DetailInfoRow(Icons.Default.Email, "Email", contact.emails.joinToString("\n"))
        }
        if (!contact.organization.isNullOrBlank()) {
            DetailInfoRow(Icons.Default.Business, "Azienda / Ruolo", contact.organization)
        }
        if (!contact.address.isNullOrBlank()) {
            DetailInfoRow(Icons.Default.LocationOn, "Indirizzo", contact.address)
        }
        if (!contact.website.isNullOrBlank()) {
            DetailInfoRow(Icons.Default.Language, "Sito Web", contact.website)
        }
        if (!contact.birthdayOrEvent.isNullOrBlank()) {
            DetailInfoRow(Icons.Default.Event, "Ricorrenza", contact.birthdayOrEvent)
        }
        if (!contact.notes.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ElectricAzure.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Notes, contentDescription = "Notizie salvate", tint = ElectricAzure)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notizie e Note Salvate",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricAzure
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
