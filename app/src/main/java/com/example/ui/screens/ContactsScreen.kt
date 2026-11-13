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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.device.DeviceContact
import com.example.ui.components.BrandedTopHeader
import com.example.ui.components.ContactColorPalette
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SuspectAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ContactListFilter(val label: String) {
    ALL("Tutti"),
    FAVORITES("Preferiti")
}

@Composable
fun ContactsScreen(
    contacts: List<DeviceContact>,
    favoriteContacts: List<DeviceContact>,
    googleAccounts: List<String>,
    lastSyncMillis: Long,
    isSyncing: Boolean,
    hasContactsPermission: Boolean,
    contactNameColorArgb: Long,
    onRequestPermissions: () -> Unit,
    onSyncGoogleContacts: () -> Unit,
    onOpenContactDetail: (DeviceContact) -> Unit,
    onToggleFavorite: (DeviceContact) -> Unit,
    onCallNumber: (String) -> Unit,
    onOpenColorSettings: () -> Unit
) {
    val gradients = LocalSpamcallerGradients.current
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ContactListFilter.ALL) }

    val resolvedNameColor = ContactColorPalette.resolveNameColor(
        argb = contactNameColorArgb,
        defaultColor = MaterialTheme.colorScheme.onSurface
    )

    val filtered = remember(contacts, favoriteContacts, query, activeFilter) {
        val base = if (activeFilter == ContactListFilter.FAVORITES) favoriteContacts else contacts
        val q = query.trim().lowercase(Locale.ITALIAN)
        if (q.isBlank()) base
        else base.filter { c ->
            c.displayName.lowercase(Locale.ITALIAN).contains(q) ||
                c.phoneNumbers.any { it.number.contains(q) || it.normalizedNumber.contains(q) } ||
                c.emails.any { it.lowercase(Locale.ITALIAN).contains(q) } ||
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
                subtitle = "${contacts.size} contatti • ${favoriteContacts.size} preferiti",
                trailing = {
                    IconButton(
                        onClick = onOpenColorSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("contacts_open_color_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Cambia colore nomi contatti",
                            tint = ElectricAzure
                        )
                    }
                }
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

        // Favorite Contacts Horizontal Carousel
        if (hasContactsPermission && favoriteContacts.isNotEmpty() && query.isBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = SuspectAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contatti Preferiti (${favoriteContacts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoriteContacts, key = { "fav_${it.contactId}" }) { fav ->
                            FavoriteContactTile(
                                contact = fav,
                                nameColor = resolvedNameColor,
                                onClick = { onOpenContactDetail(fav) },
                                onCallClick = {
                                    fav.phoneNumbers.firstOrNull()?.number?.let(onCallNumber)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Search Bar + Filter Chips
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca per nome, numero, email o note…") },
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
                        .testTag("contacts_search_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == ContactListFilter.ALL,
                        onClick = { activeFilter = ContactListFilter.ALL },
                        label = { Text("Tutti (${contacts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricAzure,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("contacts_filter_all")
                    )
                    FilterChip(
                        selected = activeFilter == ContactListFilter.FAVORITES,
                        onClick = { activeFilter = ContactListFilter.FAVORITES },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (activeFilter == ContactListFilter.FAVORITES) Color.White else SuspectAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Preferiti (${favoriteContacts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricAzure,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("contacts_filter_favorites")
                    )
                }
            }
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
                            text = "Concedi l'accesso ai Contatti per mostrare i tuoi contatti Google con le foto profilo, preferiti e tutte le note salvate.",
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
                            imageVector = if (activeFilter == ContactListFilter.FAVORITES) {
                                Icons.Outlined.StarBorder
                            } else {
                                Icons.Default.AccountCircle
                            },
                            contentDescription = null,
                            tint = if (activeFilter == ContactListFilter.FAVORITES) SuspectAmber else ElectricAzure,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = if (activeFilter == ContactListFilter.FAVORITES) {
                                "Nessun contatto preferito"
                            } else {
                                "Nessun contatto trovato"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (activeFilter == ContactListFilter.FAVORITES) {
                                "Tocca la stella accanto a un contatto per aggiungerlo subito ai tuoi preferiti."
                            } else {
                                "Tocca Sincronizza per importare i contatti salvati sul tuo account Google."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.contactId }) { contact ->
                ContactRowCard(
                    contact = contact,
                    nameColor = resolvedNameColor,
                    onClick = { onOpenContactDetail(contact) },
                    onToggleFavorite = { onToggleFavorite(contact) },
                    onCallClick = {
                        contact.phoneNumbers.firstOrNull()?.number?.let(onCallNumber)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteContactTile(
    contact: DeviceContact,
    nameColor: Color,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val primaryPhone = contact.phoneNumbers.firstOrNull()?.number
    Surface(
        modifier = Modifier
            .width(122.dp)
            .clickable(onClick = onClick)
            .testTag("favorite_tile_${contact.contactId}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ContactOrCallerAvatar(
                    name = contact.displayName,
                    photoUri = contact.photoUri,
                    size = 64.dp
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Preferito",
                        tint = SuspectAmber,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(14.dp)
                    )
                }
            }

            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (primaryPhone != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SafeEmerald.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCallClick)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Chiama ${contact.displayName}",
                            tint = SafeEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chiama",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SafeEmerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRowCard(
    contact: DeviceContact,
    nameColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCallClick: () -> Unit
) {
    val primaryNumber = contact.phoneNumbers.firstOrNull()?.number.orEmpty()
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("contact_row_${contact.contactId}"),
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
                        color = nameColor,
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
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_fav_btn_${contact.contactId}")
            ) {
                Icon(
                    imageVector = if (contact.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (contact.isStarred) {
                        "Rimuovi ${contact.displayName} dai preferiti"
                    } else {
                        "Aggiungi ${contact.displayName} ai preferiti"
                    },
                    tint = if (contact.isStarred) SuspectAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
