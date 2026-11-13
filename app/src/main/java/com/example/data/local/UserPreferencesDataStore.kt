package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val lastContactsSyncMillis: Long = 0L,
    /**
     * 0L means automatic (uses MaterialTheme.colorScheme.onSurface).
     * Non-zero stores an ARGB Long (e.g. 0xFF38BDF8L).
     */
    val contactNameColorArgb: Long = 0L,
    /**
     * Local favorite overrides keyed by Android Contact ID (+id = favorite, -id = not favorite),
     * ensuring favorites work immediately even if WRITE_CONTACTS isn't granted or for read-only accounts.
     */
    val favoriteOverrides: Map<Long, Boolean> = emptyMap()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_BLOCK = booleanPreferencesKey("auto_block_enabled")
        val SPAM_THRESHOLD = intPreferencesKey("spam_threshold")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden_numbers")
        val LAST_BACKUP = stringPreferencesKey("last_phone_backup_label")
        val LAST_SYNC = longPreferencesKey("last_contacts_sync")
        val CONTACT_NAME_COLOR = longPreferencesKey("contact_name_color_argb")
        val FAVORITE_OVERRIDES = stringSetPreferencesKey("favorite_contact_overrides")
    }

    val preferencesFlow: Flow<ScreeningPreferences> = context.spamcallerDataStore.data.map { prefs ->
        val rawOverrides = prefs[Keys.FAVORITE_OVERRIDES] ?: emptySet()
        val parsedOverrides = buildMap {
            rawOverrides.forEach { token ->
                val isFav = token.startsWith("+")
                val id = token.drop(1).toLongOrNull()
                if (id != null) {
                    put(id, isFav)
                }
            }
        }
        ScreeningPreferences(
            autoBlockEnabled = prefs[Keys.AUTO_BLOCK] ?: true,
            spamThreshold = prefs[Keys.SPAM_THRESHOLD] ?: 70,
            blockHiddenNumbers = prefs[Keys.BLOCK_HIDDEN] ?: false,
            lastPhoneBackupLabel = prefs[Keys.LAST_BACKUP] ?: "Nessun backup",
            lastContactsSyncMillis = prefs[Keys.LAST_SYNC] ?: 0L,
            contactNameColorArgb = prefs[Keys.CONTACT_NAME_COLOR] ?: 0L,
            favoriteOverrides = parsedOverrides
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

    suspend fun saveContactNameColor(argb: Long) {
        context.spamcallerDataStore.edit { prefs ->
            prefs[Keys.CONTACT_NAME_COLOR] = argb
        }
    }

    suspend fun setFavoriteOverride(contactId: Long, isFavorite: Boolean) {
        context.spamcallerDataStore.edit { prefs ->
            val current = (prefs[Keys.FAVORITE_OVERRIDES] ?: emptySet())
                .filterNot { it == "+$contactId" || it == "-$contactId" }
                .toMutableSet()
            current.add(if (isFavorite) "+$contactId" else "-$contactId")
            prefs[Keys.FAVORITE_OVERRIDES] = current
        }
    }

    suspend fun restoreFavoriteOverrides(overrides: Map<Long, Boolean>) {
        context.spamcallerDataStore.edit { prefs ->
            val current = (prefs[Keys.FAVORITE_OVERRIDES] ?: emptySet()).toMutableSet()
            overrides.forEach { (id, fav) ->
                current.remove("+$id")
                current.remove("-$id")
                current.add(if (fav) "+$id" else "-$id")
            }
            prefs[Keys.FAVORITE_OVERRIDES] = current
        }
    }
}
