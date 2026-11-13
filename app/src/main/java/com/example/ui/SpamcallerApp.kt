package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.GoogleAuthHelper
import com.example.data.device.SystemRoleHelper
import com.example.ui.screens.BackupSystemScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.ProtectionScreen
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LocalSpamcallerGradients
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SpamcallerTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CALLS("calls", "Chiamate", Icons.Filled.Call, Icons.Outlined.Call),
    CONTACTS("contacts", "Contatti", Icons.Filled.Contacts, Icons.Outlined.Contacts),
    PROTECTION("protection", "Protezione", Icons.Filled.Shield, Icons.Outlined.Shield),
    BACKUP("backup", "Backup", Icons.Filled.Save, Icons.Outlined.Save)
}

@Composable
fun SpamcallerApp(
    currentUserId: String,
    initialDialNumber: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val gradients = LocalSpamcallerGradients.current

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = MainViewModel.factory(currentUserId)
    )

    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle()
    val recentCalls by viewModel.recentCalls.collectAsStateWithLifecycle()
    val callFilter by viewModel.callFilter.collectAsStateWithLifecycle()
    val lookupState by viewModel.lookupState.collectAsStateWithLifecycle()
    val spamSources by viewModel.spamSources.collectAsStateWithLifecycle()
    val blockedNumbers by viewModel.blockedNumbers.collectAsStateWithLifecycle()
    val lookupCache by viewModel.lookupCacheCount.collectAsStateWithLifecycle()
    val backupRecords by viewModel.backupRecords.collectAsStateWithLifecycle()
    val preferences by viewModel.screeningPreferences.collectAsStateWithLifecycle()
    val roleStatus by viewModel.roleStatus.collectAsStateWithLifecycle()
    val cloudProfile by viewModel.cloudProfile.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingContacts.collectAsStateWithLifecycle()

    var hasContactsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallLogPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasContactsPerm = result[Manifest.permission.READ_CONTACTS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        hasCallLogPerm = result[Manifest.permission.READ_CALL_LOG] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.refreshDeviceData(hasContactsPerm, hasCallLogPerm)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshRoleStatus()
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToPhoneUri(uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreFromPhoneUri(uri)
        }
    }

    LaunchedEffect(hasContactsPerm, hasCallLogPerm) {
        viewModel.refreshDeviceData(hasContactsPerm, hasCallLogPerm)
        viewModel.refreshRoleStatus()
    }

    LaunchedEffect(initialDialNumber) {
        if (!initialDialNumber.isNullOrBlank()) {
            viewModel.lookupOnlineNumber(initialDialNumber)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SpamcallerTab.CALLS.route

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.screenBackground)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        SpamcallerTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(SpamcallerTab.CALLS.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricAzure,
                                    selectedTextColor = ElectricAzure,
                                    indicatorColor = ElectricAzure.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        SpamcallerTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(SpamcallerTab.CALLS.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = SpamcallerTab.CALLS.route
                    ) {
                        composable(SpamcallerTab.CALLS.route) {
                            CallsScreen(
                                recentCalls = recentCalls,
                                contacts = contacts,
                                callFilter = callFilter,
                                lookupState = lookupState,
                                roleStatus = roleStatus,
                                hasCallLogPermission = hasCallLogPerm,
                                onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
                                onFilterSelected = viewModel::setCallFilter,
                                onSearchOnline = { num -> viewModel.lookupOnlineNumber(num) },
                                onClearSearch = viewModel::clearLookupState,
                                onPlaceCall = { num -> SystemRoleHelper.placePhoneCall(context, num) },
                                onBlockNumber = { num, name, reason, score ->
                                    viewModel.blockNumber(num, name, reason, "Registro", score)
                                },
                                onActivateSystemRoleClick = {
                                    navController.navigate(SpamcallerTab.BACKUP.route)
                                }
                            )
                        }

                        composable(SpamcallerTab.CONTACTS.route) {
                            ContactsScreen(
                                contacts = contacts,
                                googleAccounts = googleAccounts,
                                lastSyncMillis = preferences.lastContactsSyncMillis,
                                isSyncing = isSyncing,
                                hasContactsPermission = hasContactsPerm,
                                onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
                                onSyncGoogleContacts = {
                                    viewModel.syncGoogleContactsNow(hasContactsPerm, hasCallLogPerm)
                                },
                                onCallNumber = { num -> SystemRoleHelper.placePhoneCall(context, num) },
                                onBlockNumber = { num, name, reason, score ->
                                    viewModel.blockNumber(num, name, reason, "Contatti", score)
                                }
                            )
                        }

                        composable(SpamcallerTab.PROTECTION.route) {
                            ProtectionScreen(
                                spamSources = spamSources,
                                blockedNumbers = blockedNumbers,
                                cachedCount = lookupCache.size,
                                preferences = preferences,
                                onToggleSource = viewModel::toggleSpamSource,
                                onAddCustomSource = viewModel::addCustomSpamSource,
                                onDeleteCustomSource = viewModel::deleteCustomSpamSource,
                                onUpdateSettings = viewModel::updateScreeningSettings,
                                onBlockManualNumber = { num, name, reason, score ->
                                    viewModel.blockNumber(num, name, reason, "Manuale", score)
                                },
                                onUnblockNumber = viewModel::unblockNumber,
                                onTestLookupOnline = { num ->
                                    viewModel.lookupOnlineNumber(num)
                                    navController.navigate(SpamcallerTab.CALLS.route)
                                }
                            )
                        }

                        composable(SpamcallerTab.BACKUP.route) {
                            val currentFirebaseUser = if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                                Firebase.auth.currentUser
                            } else null
                            BackupSystemScreen(
                                firebaseUser = currentFirebaseUser,
                                cloudProfile = cloudProfile,
                                roleStatus = roleStatus,
                                backupRecords = backupRecords,
                                onRequestDefaultDialerRole = {
                                    SystemRoleHelper.createDefaultDialerIntent(context)
                                        ?.let { roleLauncher.launch(it) }
                                },
                                onRequestCallScreeningRole = {
                                    SystemRoleHelper.createCallScreeningIntent(context)
                                        ?.let { roleLauncher.launch(it) }
                                },
                                onCreateBackupNow = viewModel::createPhoneBackupNow,
                                onExportBackupToPhoneFolder = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
                                        .format(Date())
                                    exportDocumentLauncher.launch("spamcaller_backup_$stamp.json")
                                },
                                onRestoreBackupFromFile = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onRestoreRecord = viewModel::restoreFromBackupRecord,
                                onDeleteRecord = viewModel::deleteLocalBackup,
                                onSignOut = {
                                    GoogleAuthHelper.signOut(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
