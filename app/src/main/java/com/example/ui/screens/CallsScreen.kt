package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.device.CallDirection
import com.example.data.device.DeviceCallLogItem
import com.example.data.device.DeviceContact
import com.example.data.device.SystemRoleStatus
import com.example.data.remote.OnlineSpamReport
import com.example.data.remote.SpamVerdict
import com.example.ui.CallFilter
import com.example.ui.LookupUiState
import com.example.ui.components.BrandedTopHeader
import com.example.ui.components.ContactOrCallerAvatar
import com.example.ui.components.SpamVerdictBadge
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.example.ui.theme.SuspectAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    recentCalls: List<DeviceCallLogItem>,
    contacts: List<DeviceContact>,
    callFilter: CallFilter,
    lookupState: LookupUiState,
    roleStatus: SystemRoleStatus,
    hasCallLogPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onFilterSelected: (CallFilter) -> Unit,
    onSearchOnline: (String) -> Unit,
    onClearSearch: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onBlockNumber: (String, String, String, Int) -> Unit,
    onActivateSystemRoleClick: () -> Unit
) {
    val context = LocalContext.current
    val gradients = LocalSpamcallerGradients.current
    var searchQuery by remember { mutableStateOf("") }
    var showDialerSheet by remember { mutableStateOf(false) }

    val filteredCalls = remember(recentCalls, callFilter, searchQuery) {
        recentCalls.filter { item ->
            val matchesFilter = when (callFilter) {
                CallFilter.ALL -> true
                CallFilter.MISSED -> item.direction == CallDirection.MISSED
                CallFilter.SPAM -> item.isBlocked ||
                    item.spamVerdict == SpamVerdict.HIGH_RISK ||
                    item.spamVerdict == SpamVerdict.SUSPICIOUS
            }
            val q = searchQuery.trim().lowercase(Locale.ITALIAN)
            val matchesSearch = q.isBlank() ||
                item.displayName.lowercase(Locale.ITALIAN).contains(q) ||
                item.number.contains(q)
            matchesFilter && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                BrandedTopHeader(
                    title = "Spamcaller",
                    subtitle = if (roleStatus.isDefaultDialer) {
                        "App Telefono di Sistema attiva"
                    } else if (roleStatus.isCallScreeningApp) {
                        "Protezione ID Chiamante attiva"
                    } else {
                        "Verifica online & registro chiamate"
                    }
                )
            }

            // Truecaller-style search + live online website lookup bar
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cerca numero, contatto o siti spam…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Cerca")
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onClearSearch()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Pulisci")
                                    }
                                    IconButton(
                                        onClick = { onSearchOnline(searchQuery) },
                                        modifier = Modifier.testTag("online_lookup_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Public,
                                            contentDescription = "Cerca sui siti spam online",
                                            tint = LuminousCyan
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricAzure,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_search_input")
                    )

                    if (searchQuery.trim().length >= 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSearchOnline(searchQuery) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("search_websites_cta")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifica \"$searchQuery\" su Tellows, ChiMiChiama & altri siti")
                        }
                    }
                }
            }

            // Live Online Lookup Result Card
            item {
                AnimatedVisibility(visible = lookupState !is LookupUiState.Idle) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        when (lookupState) {
                            is LookupUiState.Loading -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = LuminousCyan,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Consultazione siti online per ${lookupState.number}…",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            is LookupUiState.Error -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = SpamCrimson.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = lookupState.message,
                                        color = SpamCrimson,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            is LookupUiState.Ready -> {
                                OnlineReportCard(
                                    report = lookupState.report,
                                    onDismiss = onClearSearch,
                                    onBlock = {
                                        onBlockNumber(
                                            lookupState.report.normalizedNumber,
                                            "Segnalato Online",
                                            lookupState.report.summary,
                                            lookupState.report.riskScore
                                        )
                                    },
                                    onCall = { onPlaceCall(lookupState.report.normalizedNumber) },
                                    onOpenUrl = { url ->
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            )
                                        }
                                    }
                                )
                            }

                            LookupUiState.Idle -> Unit
                        }
                    }
                }
            }

            // System Role Prompt Card if not yet chosen by user
            if (!roleStatus.isDefaultDialer || !roleStatus.isCallScreeningApp) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(gradients.heroBlackBlue, RoundedCornerShape(20.dp))
                            .border(1.dp, gradients.accentBorderBrush, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Protezione di Sistema",
                                    tint = LuminousCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Imposta Spamcaller sul telefono",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Puoi scegliere se rendere Spamcaller la tua app Telefono di sistema per chiamare/ricevere o attivarla per il blocco chiamate spam.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFDBEAFE)
                            )
                            Button(
                                onClick = onActivateSystemRoleClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricAzure),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("activate_system_role_banner_btn")
                            ) {
                                Text("Scegli Impostazioni di Sistema")
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CallFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = callFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.labelIt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricAzure,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("call_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (!hasCallLogPermission) {
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
                                text = "Accesso al registro chiamate necessario",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Concedi i permessi Telefono e Registro Chiamate per visualizzare le chiamate recenti con foto Google e verifica spam automatica.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onRequestPermissions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("grant_call_permissions_btn")
                            ) {
                                Text("Concedi Permessi Chiamate")
                            }
                        }
                    }
                }
            } else if (filteredCalls.isEmpty()) {
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
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = null,
                                tint = ElectricAzure,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Nessuna chiamata trovata",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Usa il tastierino blu in basso a destra per effettuare una chiamata o cerca un numero in alto sui siti spam.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredCalls, key = { it.id }) { callItem ->
                    CallLogRowCard(
                        item = callItem,
                        onCallClick = { onPlaceCall(callItem.number) },
                        onInspectOnline = { onSearchOnline(callItem.number) },
                        onBlockClick = {
                            onBlockNumber(
                                callItem.number,
                                callItem.displayName,
                                "Bloccato dal registro chiamate",
                                95
                            )
                        }
                    )
                }
            }
        }

        // Truecaller-style floating T9 dialer button
        FloatingActionButton(
            onClick = { showDialerSheet = true },
            containerColor = ElectricAzure,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(62.dp)
                .testTag("open_dialer_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = "Apri tastierino telefonico",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showDialerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDialerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TruecallerDialerPad(
                contacts = contacts,
                onCallNumber = { num ->
                    showDialerSheet = false
                    onPlaceCall(num)
                },
                onCheckOnline = { num ->
                    showDialerSheet = false
                    searchQuery = num
                    onSearchOnline(num)
                }
            )
        }
    }
}

@Composable
private fun CallLogRowCard(
    item: DeviceCallLogItem,
    onCallClick: () -> Unit,
    onInspectOnline: () -> Unit,
    onBlockClick: () -> Unit
) {
    val isSpam = item.isBlocked || item.spamVerdict == SpamVerdict.HIGH_RISK
    val formattedDate = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY).format(Date(item.timestamp))
    }
    val dirIcon = when (item.direction) {
        CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
        CallDirection.BLOCKED, CallDirection.REJECTED -> Icons.Default.Block
    }
    val dirColor = when (item.direction) {
        CallDirection.MISSED, CallDirection.BLOCKED, CallDirection.REJECTED -> SpamCrimson
        CallDirection.OUTGOING -> LuminousCyan
        CallDirection.INCOMING -> SafeEmerald
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = onInspectOnline),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactOrCallerAvatar(
                name = item.displayName,
                photoUri = item.photoUri,
                isSpam = isSpam,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = item.direction.labelIt,
                        tint = dirColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${item.number} • $formattedDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.spamScore > 0 || item.isBlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SpamVerdictBadge(verdict = item.spamVerdict, score = item.spamScore)
                }
            }
            IconButton(
                onClick = onBlockClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocca numero",
                    tint = if (isSpam) SpamCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onCallClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Chiama ${item.displayName}",
                    tint = ElectricAzure
                )
            }
        }
    }
}

@Composable
private fun OnlineReportCard(
    report: OnlineSpamReport,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onCall: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val gradients = LocalSpamcallerGradients.current
    val accent = when (report.verdict) {
        SpamVerdict.HIGH_RISK -> SpamCrimson
        SpamVerdict.SUSPICIOUS -> SuspectAmber
        SpamVerdict.SAFE -> SafeEmerald
        SpamVerdict.UNKNOWN -> LuminousCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradients.cardBlackBlue, RoundedCornerShape(22.dp))
            .border(1.5.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.normalizedNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = report.verdict.labelIt,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
                SpamVerdictBadge(verdict = report.verdict, score = report.riskScore)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                }
            }

            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDBEAFE)
            )

            report.sources.forEach { src ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUrl(src.url) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (src.reachable) Icons.Default.CheckCircle else Icons.Default.Public,
                            contentDescription = null,
                            tint = if (src.detectedScore >= 60) SpamCrimson else LuminousCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${src.sourceName} (${src.latencyMs}ms)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text(
                                text = src.snippet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF93C5FD),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Apri sito",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBlock,
                    enabled = !report.isManuallyBlocked,
                    colors = ButtonDefaults.buttonColors(containerColor = SpamCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (report.isManuallyBlocked) "Bloccato" else "Blocca")
                }
                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chiama", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TruecallerDialerPad(
    contacts: List<DeviceContact>,
    onCallNumber: (String) -> Unit,
    onCheckOnline: (String) -> Unit
) {
    var digits by remember { mutableStateOf("") }

    val matchingContacts = remember(digits, contacts) {
        if (digits.length < 2) emptyList()
        else contacts.filter { c ->
            c.phoneNumbers.any { it.normalizedNumber.contains(digits) } ||
                c.displayName.contains(digits, ignoreCase = true)
        }.take(3)
    }

    val dialKeys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        matchingContacts.forEach { contact ->
            val firstPhone = contact.phoneNumbers.firstOrNull()?.number.orEmpty()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCallNumber(firstPhone) }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactOrCallerAvatar(contact.displayName, contact.photoUri, size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                        Text(firstPhone, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Default.Call, contentDescription = "Chiama", tint = ElectricAzure)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = digits.ifBlank { "Digita il numero" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (digits.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (digits.isNotEmpty()) {
                IconButton(onClick = { digits = digits.dropLast(1) }) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Cancella cifra")
                }
            }
        }

        dialKeys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (num, letters) ->
                    Surface(
                        modifier = Modifier
                            .size(width = 88.dp, height = 58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { digits += num },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = num,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (letters.isNotEmpty()) {
                                Text(
                                    text = letters,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = { if (digits.length >= 3) onCheckOnline(digits) },
                enabled = digits.length >= 3,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verifica Spam")
            }
            Button(
                onClick = { if (digits.isNotBlank()) onCallNumber(digits) },
                enabled = digits.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeEmerald),
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp)
                    .testTag("dialer_call_btn")
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chiama", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
