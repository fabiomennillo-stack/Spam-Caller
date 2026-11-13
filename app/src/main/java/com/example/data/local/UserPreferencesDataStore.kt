package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.spamcallerDataStore by preferencesDataStore(name = "spamcaller_prefs")

data class ScreeningPreferences(
    val autoBlockEnabled: Boolean = true,
    val spamThreshold: Int = 70,
    val blockHiddenNumbers: Boolean = false,
    val lastPhoneBackupLabel: String = "Nessun backup",
    val lastContactsSyncMillis: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_BLOCK = booleanPreferencesKey("auto_block_enabled")
        val SPAM_THRESHOLD = intPreferencesKey("spam_threshold")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden_numbers")
        val LAST_BACKUP = stringPreferencesKey("last_phone_backup_label")
        val LAST_SYNC = androidx.datastore.preferences.core.longPreferencesKey("last_contacts_sync")
    }

    val preferencesFlow: Flow<ScreeningPreferences> = context.spamcallerDataStore.data.map { prefs ->
        ScreeningPreferences(
            autoBlockEnabled = prefs[Keys.AUTO_BLOCK] ?: true,
            spamThreshold = prefs[Keys.SPAM_THRESHOLD] ?: 70,
            blockHiddenNumbers = prefs[Keys.BLOCK_HIDDEN] ?: false,
            lastPhoneBackupLabel = prefs[Keys.LAST_BACKUP] ?: "Nessun backup",
            lastContactsSyncMillis = prefs[Keys.LAST_SYNC] ?: 0L
        )
    }

    suspend fun getPreferencesOnce(): ScreeningPreferences = preferencesFlow.first()

    suspend fun saveScreeningSettings(
        autoBlockEnabled: Boolean,
        spamThreshold: Int,
        blockHiddenNumbers: Boolean
    ) {
        context.spamcallerDataStore.edit { prefs ->
            prefs[Keys.AUTO_BLOCK] = autoBlockEnabled
            prefs[Keys.SPAM_THRESHOLD] = spamThreshold.coerceIn(10, 100)
            prefs[Keys.BLOCK_HIDDEN] = blockHiddenNumbers
        }
    }

    suspend fun saveLastBackupLabel(label: String) {
        context.spamcallerDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP] = label
        }
    }

    suspend fun saveLastContactsSync(timestamp: Long) {
        context.spamcallerDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC] = timestamp
        }
    }
}
