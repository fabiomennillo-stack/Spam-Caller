package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.R
import com.example.data.device.DeviceCallLogItem
import com.example.data.device.DeviceCallLogRepository
import com.example.data.device.DeviceContact
import com.example.data.device.GoogleContactsRepository
import com.example.data.device.SystemRoleHelper
import com.example.data.device.SystemRoleStatus
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.LocalBackupManager
import com.example.data.local.LocalBackupRecordEntity
import com.example.data.local.ScreeningPreferences
import com.example.data.local.SpamSourceEntity
import com.example.data.local.SpamcallerDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.OnlineSpamLookupService
import com.example.data.remote.OnlineSpamReport
import com.example.data.remote.UserProfile
import com.example.data.remote.UserProfileRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LookupUiState {
    data object Idle : LookupUiState
    data class Loading(val number: String) : LookupUiState
    data class Ready(val report: OnlineSpamReport) : LookupUiState
    data class Error(val message: String) : LookupUiState
}

enum class CallFilter(val labelIt: String) {
    ALL("Tutte"),
    MISSED("Perse"),
    SPAM("Spam / Bloccate")
}

class MainViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val db: SpamcallerDatabase,
    private val prefsRepo: UserPreferencesRepository,
    private val userProfileRepo: UserProfileRepository?,
    private val lookupService: OnlineSpamLookupService,
    private val contactsRepo: GoogleContactsRepository,
    private val callLogRepo: DeviceCallLogRepository,
    private val backupManager: LocalBackupManager
) : ViewModel() {

    private val dao = db.spamcallerDao()

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = dao.observeBlockedNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val spamSources: StateFlow<List<SpamSourceEntity>> = dao.observeSpamSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val backupRecords: StateFlow<List<LocalBackupRecordEntity>> = dao.observeBackupRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val lookupCacheCount: StateFlow<List<com.example.data.local.SpamLookupCacheEntity>> =
        dao.observeLookupCache()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val screeningPreferences: StateFlow<ScreeningPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ScreeningPreferences())

    val cloudProfile: StateFlow<UserProfile?> = (userProfileRepo?.observeProfile(currentUserId)
        ?: kotlinx.coroutines.flow.flowOf(null))
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Impossibile osservare il profilo cloud", firestoreError ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contacts: StateFlow<List<DeviceContact>> = _contacts.asStateFlow()

    private val _googleAccounts = MutableStateFlow<List<String>>(emptyList())
    val googleAccounts: StateFlow<List<String>> = _googleAccounts.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<DeviceCallLogItem>>(emptyList())
    val recentCalls: StateFlow<List<DeviceCallLogItem>> = _recentCalls.asStateFlow()

    private val _callFilter = MutableStateFlow(CallFilter.ALL)
    val callFilter: StateFlow<CallFilter> = _callFilter.asStateFlow()

    private val _lookupState = MutableStateFlow<LookupUiState>(LookupUiState.Idle)
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()

    private val _roleStatus = MutableStateFlow(SystemRoleHelper.getRoleStatus(appContext))
    val roleStatus: StateFlow<SystemRoleStatus> = _roleStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSyncingContacts = MutableStateFlow(false)
    val isSyncingContacts: StateFlow<Boolean> = _isSyncingContacts.asStateFlow()

    init {
        viewModelScope.launch {
            lookupService.ensureDefaultSourcesSeeded()
            if (userProfileRepo != null && com.google.firebase.FirebaseApp.getApps(appContext).isNotEmpty()) {
                val user = Firebase.auth.currentUser
                userProfileRepo.ensureProfileInitialized(
                    displayName = user?.displayName ?: "Utente Spamcaller",
                    email = user?.email.orEmpty(),
                    photoUrl = user?.photoUrl?.toString().orEmpty()
                )
            }
        }
        refreshRoleStatus()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshRoleStatus() {
        _roleStatus.value = SystemRoleHelper.getRoleStatus(appContext)
    }

    fun setCallFilter(filter: CallFilter) {
        _callFilter.value = filter
    }

    fun refreshDeviceData(hasContactsPerm: Boolean, hasCallLogPerm: Boolean) {
        viewModelScope.launch {
            _googleAccounts.value = contactsRepo.getGoogleAccounts().map { it.name }
            val loadedContacts = if (hasContactsPerm) {
                contactsRepo.loadContacts()
            } else {
                emptyList()
            }
            _contacts.value = loadedContacts

            if (hasCallLogPerm) {
                _recentCalls.value = callLogRepo.loadRecentCalls(loadedContacts)
            } else {
                _recentCalls.value = emptyList()
            }
        }
    }

    fun syncGoogleContactsNow(hasContactsPerm: Boolean, hasCallLogPerm: Boolean) {
        viewModelScope.launch {
            _isSyncingContacts.value = true
            val requested = contactsRepo.requestGoogleContactsSync()
            prefsRepo.saveLastContactsSync(System.currentTimeMillis())
            refreshDeviceData(hasContactsPerm, hasCallLogPerm)
            _isSyncingContacts.value = false
            _statusMessage.value = if (requested) {
                "Sincronizzazione contatti Google avviata e rubrica aggiornata."
            } else {
                "Rubrica aggiornata. Verifica che la sincronizzazione Google sia attiva nelle impostazioni del dispositivo."
            }
        }
    }

    fun lookupOnlineNumber(rawNumber: String, forceRefresh: Boolean = true) {
        val cleaned = rawNumber.trim()
        if (cleaned.length < 3) {
            _lookupState.value = LookupUiState.Error("Inserisci almeno 3 cifre per la ricerca online.")
            return
        }
        viewModelScope.launch {
            _lookupState.value = LookupUiState.Loading(cleaned)
            runCatching {
                lookupService.lookupNumber(cleaned, forceRefresh = forceRefresh)
            }.onSuccess { report ->
                _lookupState.value = LookupUiState.Ready(report)
            }.onFailure { err ->
                _lookupState.value = LookupUiState.Error(
                    err.localizedMessage ?: "Errore durante la consultazione dei siti spam."
                )
            }
        }
    }

    fun clearLookupState() {
        _lookupState.value = LookupUiState.Idle
    }

    fun blockNumber(
        rawNumber: String,
        displayName: String = "Numero Spam",
        reason: String = "Bloccato manualmente",
        source: String = "Utente",
        riskScore: Int = 95
    ) {
        viewModelScope.launch {
            val normalized = OnlineSpamLookupService.normalizePhoneNumber(rawNumber)
            if (normalized.isBlank()) return@launch
            dao.upsertBlockedNumber(
                BlockedNumberEntity(
                    phoneNumber = normalized,
                    displayName = displayName.ifBlank { normalized },
                    reason = reason,
                    source = source,
                    riskScore = riskScore
                )
            )
            _statusMessage.value = "Numero $normalized aggiunto alla lista nera."
            val currentLookup = _lookupState.value
            if (currentLookup is LookupUiState.Ready &&
                currentLookup.report.normalizedNumber == normalized
            ) {
                _lookupState.value = LookupUiState.Ready(
                    currentLookup.report.copy(isManuallyBlocked = true, riskScore = 100)
                )
            }
        }
    }

    fun unblockNumber(phoneNumber: String) {
        viewModelScope.launch {
            val normalized = OnlineSpamLookupService.normalizePhoneNumber(phoneNumber)
            dao.deleteBlockedNumber(normalized)
            _statusMessage.value = "Numero $normalized rimosso dai bloccati."
        }
    }

    fun toggleSpamSource(source: SpamSourceEntity, enabled: Boolean) {
        viewModelScope.launch {
            dao.updateSpamSource(source.copy(isEnabled = enabled))
        }
    }

    fun addCustomSpamSource(name: String, urlInput: String, keywords: String) {
        viewModelScope.launch {
            val cleanName = name.trim()
            var cleanUrl = urlInput.trim()
            if (cleanName.isBlank() || cleanUrl.isBlank()) {
                _statusMessage.value = "Inserisci nome sito e indirizzo URL."
                return@launch
            }
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            if (!cleanUrl.contains("{number}")) {
                cleanUrl = cleanUrl.trimEnd('/') + "/{number}"
            }
            dao.insertSpamSource(
                SpamSourceEntity(
                    name = cleanName,
                    urlTemplate = cleanUrl,
                    category = "Sito indicato da te",
                    keywordsCsv = keywords.ifBlank { "spam,truffa,telemarketing,molesto" },
                    isEnabled = true,
                    isBuiltIn = false,
                    priority = 10
                )
            )
            _statusMessage.value = "Sito \"$cleanName\" aggiunto ai controlli automatici!"
        }
    }

    fun deleteCustomSpamSource(source: SpamSourceEntity) {
        viewModelScope.launch {
            if (!source.isBuiltIn) {
                dao.deleteCustomSpamSource(source.id)
                _statusMessage.value = "Sito \"${source.name}\" rimosso."
            }
        }
    }

    fun updateScreeningSettings(
        autoBlockEnabled: Boolean,
        spamThreshold: Int,
        blockHiddenNumbers: Boolean
    ) {
        viewModelScope.launch {
            prefsRepo.saveScreeningSettings(autoBlockEnabled, spamThreshold, blockHiddenNumbers)
            userProfileRepo?.updateSettings(autoBlockEnabled, spamThreshold, blockHiddenNumbers)
        }
    }

    fun createPhoneBackupNow() {
        viewModelScope.launch {
            val prefs = prefsRepo.getPreferencesOnce()
            runCatching {
                backupManager.saveBackupOnPhone(
                    autoBlockEnabled = prefs.autoBlockEnabled,
                    spamThreshold = prefs.spamThreshold,
                    blockHiddenNumbers = prefs.blockHiddenNumbers
                )
            }.onSuccess { summary ->
                prefsRepo.saveLastBackupLabel(summary.fileName)
                userProfileRepo?.updateSettings(
                    prefs.autoBlockEnabled,
                    prefs.spamThreshold,
                    prefs.blockHiddenNumbers,
                    lastPhoneBackupLabel = summary.fileName
                )
                _statusMessage.value =
                    "Backup salvato sul telefono: ${summary.fileName} (${summary.blockedCount} bloccati, ${summary.sourcesCount} siti)."
            }.onFailure { err ->
                _statusMessage.value = "Errore salvataggio backup: ${err.localizedMessage}"
            }
        }
    }

    fun exportBackupToPhoneUri(uri: Uri) {
        viewModelScope.launch {
            val prefs = prefsRepo.getPreferencesOnce()
            runCatching {
                backupManager.exportBackupToUri(
                    uri = uri,
                    autoBlockEnabled = prefs.autoBlockEnabled,
                    spamThreshold = prefs.spamThreshold,
                    blockHiddenNumbers = prefs.blockHiddenNumbers
                )
            }.onSuccess { summary ->
                prefsRepo.saveLastBackupLabel(summary.fileName)
                _statusMessage.value =
                    "Backup esportato nella memoria del telefono (${summary.fileName})."
            }.onFailure { err ->
                _statusMessage.value = "Errore esportazione backup: ${err.localizedMessage}"
            }
        }
    }

    fun restoreFromBackupRecord(record: LocalBackupRecordEntity) {
        viewModelScope.launch {
            runCatching {
                backupManager.restoreFromLocalFile(record.filePath)
            }.onSuccess { (summary, settings) ->
                if (settings != null) {
                    prefsRepo.saveScreeningSettings(settings.first, settings.second, settings.third)
                }
                _statusMessage.value =
                    "Ripristinati ${summary.blockedRestored} numeri, ${summary.sourcesRestored} nuovi siti e ${summary.cacheRestored} schede spam."
            }.onFailure { err ->
                _statusMessage.value = "Errore ripristino: ${err.localizedMessage}"
            }
        }
    }

    fun restoreFromPhoneUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                backupManager.restoreFromUri(uri)
            }.onSuccess { (summary, settings) ->
                if (settings != null) {
                    prefsRepo.saveScreeningSettings(settings.first, settings.second, settings.third)
                }
                _statusMessage.value =
                    "Ripristino completato dal file: ${summary.blockedRestored} numeri e ${summary.sourcesRestored} siti."
            }.onFailure { err ->
                _statusMessage.value = "File backup non valido: ${err.localizedMessage}"
            }
        }
    }

    fun deleteLocalBackup(record: LocalBackupRecordEntity) {
        viewModelScope.launch {
            backupManager.deleteLocalBackup(record)
            _statusMessage.value = "Backup ${record.fileName} eliminato."
        }
    }

    companion object {
        private const val TAG = "MainViewModel"

        fun factory(currentUserId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    val app = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ) as Application
                    val db = SpamcallerDatabase.getInstance(app)
                    val dao = db.spamcallerDao()
                    val repo = if (com.google.firebase.FirebaseApp.getApps(app).isNotEmpty()) {
                        val databaseId = app.getString(R.string.firestore_database_id)
                        UserProfileRepository(FirebaseFirestore.getInstance(databaseId))
                    } else {
                        null
                    }
                    return MainViewModel(
                        appContext = app.applicationContext,
                        currentUserId = currentUserId,
                        db = db,
                        prefsRepo = UserPreferencesRepository(app),
                        userProfileRepo = repo,
                        lookupService = OnlineSpamLookupService(dao),
                        contactsRepo = GoogleContactsRepository(app),
                        callLogRepo = DeviceCallLogRepository(app, dao),
                        backupManager = LocalBackupManager(app, dao)
                    ) as T
                }
            }
    }
}
