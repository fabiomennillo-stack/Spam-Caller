package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.device.DeviceContact
import com.example.ui.components.ContactColorPalette
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.example.ui.theme.SuspectAmber

private val WhatsAppGreen = Color(0xFF25D366)
private val WhatsAppDarkGreen = Color(0xFF128C7E)

private enum class NumberPickerMode {
    PHONE_CALL,
    WHATSAPP_CALL,
    WHATSAPP_CHAT
}

@Composable
fun ContactDetailScreen(
    contact: DeviceContact,
    contactNameColorArgb: Long,
    onBack: () -> Unit,
    onToggleFavorite: (DeviceContact) -> Unit,
    onCallPhone: (String) -> Unit,
    onCallWhatsApp: (String, Long?) -> Unit,
    onChatWhatsApp: (String, Long?) -> Unit,
    onSendEmail: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditInContacts: (DeviceContact) -> Unit,
    onBlockNumber: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val gradients = LocalSpamcallerGradients.current
    val heroNameColor = ContactColorPalette.resolveHeroNameColor(contactNameColorArgb)

    var showZoomedPhoto by remember { mutableStateOf(false) }
    var numberPickerMode by remember { mutableStateOf<NumberPickerMode?>(null) }
    var showEmailPicker by remember { mutableStateOf(false) }

    val primaryPhone = contact.phoneNumbers.firstOrNull()?.number
    val primaryEmail = contact.emails.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Large Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradients.heroBlackBlue)
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Navigation & Actions Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("contact_detail_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Torna ai contatti",
                                    tint = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavorite(contact) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("contact_detail_favorite_btn")
                                ) {
                                    Icon(
                                        imageVector = if (contact.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (contact.isStarred) {
                                            "Rimuovi dai preferiti"
                                        } else {
                                            "Aggiungi ai preferiti"
                                        },
                                        tint = if (contact.isStarred) SuspectAmber else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onEditInContacts(contact) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("contact_detail_edit_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifica contatto",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Large Contact Photo
                        Box(
                            modifier = Modifier
                                .size(138.dp)
                                .clip(CircleShape)
                                .border(3.dp, LuminousCyan.copy(alpha = 0.7f), CircleShape)
                                .clickable(enabled = !contact.photoUri.isNullOrBlank()) {
                                    showZoomedPhoto = true
                                }
                                .testTag("contact_detail_large_avatar"),
                            contentAlignment = Alignment.Center
                        ) {
                            ContactOrCallerAvatar(
                                name = contact.displayName,
                                photoUri = contact.photoUri,
                                size = 138.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Contact Name (using customizable color)
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = heroNameColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .testTag("contact_detail_name")
                        )

                        if (!contact.nickname.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${contact.nickname}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = LuminousCyan,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (!contact.organization.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.organization,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFBFDBFE),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (contact.isStarred) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SuspectAmber.copy(alpha = 0.22f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = SuspectAmber,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Preferito",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SuspectAmber
                                        )
                                    }
                                }
                            }
                            if (!contact.googleAccountName.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ElectricAzure.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Google: ${contact.googleAccountName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFDBEAFE),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary 3-Way Action Choice: Phone Call / WhatsApp / Email
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Scegli come contattare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BigContactActionCard(
                            icon = Icons.Default.Call,
                            title = "Telefono",
                            subtitle = if (primaryPhone != null) "Chiama ora" else "Nessun numero",
                            accent = SafeEmerald,
                            enabled = primaryPhone != null,
                            testTag = "detail_action_phone_call",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (contact.phoneNumbers.size > 1) {
                                    numberPickerMode = NumberPickerMode.PHONE_CALL
                                } else if (primaryPhone != null) {
                                    onCallPhone(primaryPhone)
                                }
                            }
                        )

                        BigContactActionCard(
                            icon = Icons.Default.Phone,
                            title = "WhatsApp",
                            subtitle = if (primaryPhone != null) "Chiama WA" else "Nessun numero",
                            accent = WhatsAppGreen,
                            enabled = primaryPhone != null,
                            testTag = "detail_action_whatsapp_call",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (contact.phoneNumbers.size > 1) {
                                    numberPickerMode = NumberPickerMode.WHATSAPP_CALL
                                } else if (primaryPhone != null) {
                                    onCallWhatsApp(primaryPhone, contact.whatsappCallDataId)
                                }
                            }
                        )

                        BigContactActionCard(
                            icon = Icons.Default.Email,
                            title = "Email",
                            subtitle = if (primaryEmail != null) "Invia mail" else "Non salvata",
                            accent = ElectricAzure,
                            enabled = primaryEmail != null,
                            testTag = "detail_action_send_email",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (contact.emails.size > 1) {
                                    showEmailPicker = true
                                } else if (primaryEmail != null) {
                                    onSendEmail(primaryEmail)
                                }
                            }
                        )
                    }

                    if (primaryPhone != null) {
                        OutlinedButton(
                            onClick = {
                                if (contact.phoneNumbers.size > 1) {
                                    numberPickerMode = NumberPickerMode.WHATSAPP_CHAT
                                } else {
                                    onChatWhatsApp(primaryPhone, contact.whatsappProfileDataId)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("detail_action_whatsapp_chat")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apri Chat WhatsApp con ${contact.displayName}",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Phone Numbers Section (All numbers with individual Phone, WhatsApp, Copy, Block actions)
            if (contact.phoneNumbers.isNotEmpty()) {
                item {
                    SectionSurfaceCard(title = "Numeri di Telefono (${contact.phoneNumbers.size})") {
                        contact.phoneNumbers.forEachIndexed { index, phone ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = phone.number,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = phone.typeLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "Numero", phone.number)
                                            onShowMessage("Numero ${phone.number} copiato.")
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copia numero ${phone.number}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onCallPhone(phone.number) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeEmerald),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chiama", maxLines = 1)
                                    }
                                    Button(
                                        onClick = { onCallWhatsApp(phone.number, contact.whatsappCallDataId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("WhatsApp", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onBlockNumber(phone.number, contact.displayName) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = "Blocca ${phone.number}",
                                            tint = SpamCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emails Section
            if (contact.emailDetails.isNotEmpty() || contact.emails.isNotEmpty()) {
                item {
                    val emailList = if (contact.emailDetails.isNotEmpty()) {
                        contact.emailDetails
                    } else {
                        contact.emails.map { com.example.data.device.ContactLabeledValue(it, "Email") }
                    }
                    SectionSurfaceCard(title = "Indirizzi Email (${emailList.size})") {
                        emailList.forEachIndexed { idx, entry ->
                            if (idx > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSendEmail(entry.value) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = ElectricAzure,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.value,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = entry.typeLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onSendEmail(entry.value) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Invia")
                                }
                            }
                        }
                    }
                }
            }

            // Saved Notes ("Notizie salvate")
            if (!contact.notes.isNullOrBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = ElectricAzure.copy(alpha = 0.12f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = "Notizie salvate",
                                tint = ElectricAzure,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notizie e Note Salvate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricAzure
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = contact.notes,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // Complete Contact Data (Organization, Addresses, Websites, Events, IM, Google Account)
            val hasExtraDetails = !contact.organization.isNullOrBlank() ||
                !contact.nickname.isNullOrBlank() ||
                contact.postalAddresses.isNotEmpty() ||
                !contact.address.isNullOrBlank() ||
                contact.websites.isNotEmpty() ||
                !contact.website.isNullOrBlank() ||
                contact.events.isNotEmpty() ||
                !contact.birthdayOrEvent.isNullOrBlank() ||
                contact.imHandles.isNotEmpty() ||
                !contact.googleAccountName.isNullOrBlank()

            if (hasExtraDetails) {
                item {
                    SectionSurfaceCard(title = "Tutti i Dati del Contatto") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!contact.nickname.isNullOrBlank()) {
                                FullDetailRow(
                                    icon = Icons.Default.Person,
                                    label = "Soprannome",
                                    value = contact.nickname
                                )
                            }
                            if (!contact.organization.isNullOrBlank()) {
                                FullDetailRow(
                                    icon = Icons.Default.Business,
                                    label = "Azienda / Ruolo",
                                    value = contact.organization
                                )
                            }

                            val addresses = if (contact.postalAddresses.isNotEmpty()) {
                                contact.postalAddresses
                            } else {
                                listOfNotNull(contact.address).map {
                                    com.example.data.device.ContactLabeledValue(it, "Indirizzo")
                                }
                            }
                            addresses.forEach { addr ->
                                FullDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    label = addr.typeLabel,
                                    value = addr.value,
                                    actionLabel = "Mappa",
                                    onClick = { onOpenMap(addr.value) }
                                )
                            }

                            val siteList = if (contact.websites.isNotEmpty()) {
                                contact.websites
                            } else {
                                listOfNotNull(contact.website)
                            }
                            siteList.forEach { url ->
                                FullDetailRow(
                                    icon = Icons.Default.Language,
                                    label = "Sito Web",
                                    value = url,
                                    actionLabel = "Apri",
                                    onClick = { onOpenWebsite(url) }
                                )
                            }

                            val eventList = if (contact.events.isNotEmpty()) {
                                contact.events
                            } else {
                                listOfNotNull(contact.birthdayOrEvent).map {
                                    com.example.data.device.ContactLabeledValue(it, "Ricorrenza")
                                }
                            }
                            eventList.forEach { ev ->
                                FullDetailRow(
                                    icon = Icons.Default.Event,
                                    label = ev.typeLabel,
                                    value = ev.value
                                )
                            }

                            contact.imHandles.forEach { im ->
                                FullDetailRow(
                                    icon = Icons.Default.Forum,
                                    label = "Messaggistica / Social",
                                    value = im
                                )
                            }

                            if (!contact.googleAccountName.isNullOrBlank()) {
                                FullDetailRow(
                                    icon = Icons.Default.AccountCircle,
                                    label = "Account Google sincronizzato",
                                    value = contact.googleAccountName
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Photo Zoom Overlay
        AnimatedVisibility(
            visible = showZoomedPhoto && !contact.photoUri.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = 0.96f))
                    .clickable { showZoomedPhoto = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = "Foto ingrandita di ${contact.displayName}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
                IconButton(
                    onClick = { showZoomedPhoto = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi foto",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Multiple Phone Number Picker Dialog
    val activePicker = numberPickerMode
    if (activePicker != null) {
        val titleText = when (activePicker) {
            NumberPickerMode.PHONE_CALL -> "Scegli numero da chiamare"
            NumberPickerMode.WHATSAPP_CALL -> "Chiama su WhatsApp"
            NumberPickerMode.WHATSAPP_CHAT -> "Apri chat WhatsApp"
        }
        AlertDialog(
            onDismissRequest = { numberPickerMode = null },
            title = { Text(titleText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.phoneNumbers.forEach { phone ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    numberPickerMode = null
                                    when (activePicker) {
                                        NumberPickerMode.PHONE_CALL -> onCallPhone(phone.number)
                                        NumberPickerMode.WHATSAPP_CALL ->
                                            onCallWhatsApp(phone.number, contact.whatsappCallDataId)
                                        NumberPickerMode.WHATSAPP_CHAT ->
                                            onChatWhatsApp(phone.number, contact.whatsappProfileDataId)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(phone.number, fontWeight = FontWeight.Bold)
                                Text(
                                    phone.typeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { numberPickerMode = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Multiple Email Picker Dialog
    if (showEmailPicker) {
        AlertDialog(
            onDismissRequest = { showEmailPicker = false },
            title = { Text("Scegli indirizzo email", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.emails.forEach { email ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showEmailPicker = false
                                    onSendEmail(email)
                                }
                        ) {
                            Text(
                                text = email,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmailPicker = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun BigContactActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgBrush = if (enabled) {
        Brush.verticalGradient(listOf(DeepNavy, ObsidianBlack))
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (enabled) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (enabled) accent.copy(alpha = 0.20f) else Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Color(0xFFBFDBFE) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionSurfaceCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ElectricAzure
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FullDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ElectricAzure,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (actionLabel != null && onClick != null) {
            TextButton(onClick = onClick) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
