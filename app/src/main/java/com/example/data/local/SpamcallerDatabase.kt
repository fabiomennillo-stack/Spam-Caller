package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedNumberEntity::class,
        SpamSourceEntity::class,
        SpamLookupCacheEntity::class,
        LocalBackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpamcallerDatabase : RoomDatabase() {
    abstract fun spamcallerDao(): SpamcallerDao

    companion object {
        @Volatile
        private var INSTANCE: SpamcallerDatabase? = null

        fun getInstance(context: Context): SpamcallerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpamcallerDatabase::class.java,
                    "spamcaller_local.db"
                ).build().also { INSTANCE = it }
            }
        }

        val DEFAULT_SPAM_SOURCES = listOf(
            SpamSourceEntity(
                name = "Tellows Italia",
                urlTemplate = "https://www.tellows.it/num/{number}",
                category = "Community Score (1-9)",
                keywordsCsv = "score,truffa,telemarketing,call center,aggressivo,sondaggio,molesto,pericoloso",
                isEnabled = true,
                isBuiltIn = true,
                priority = 1
            ),
            SpamSourceEntity(
                name = "Chi Mi Chiama",
                urlTemplate = "https://www.chimichiama.it/numero/{number}",
                category = "Segnalazioni Italia",
                keywordsCsv = "spam,truffa,telemarketing,negativo,pericoloso,molesto,scocciatore",
                isEnabled = true,
                isBuiltIn = true,
                priority = 2
            ),
            SpamSourceEntity(
                name = "ChiChiama",
                urlTemplate = "https://www.chichiama.it/numero/{number}",
                category = "Database Telefonico",
                keywordsCsv = "spam,call center,telemarketing,truffa,indesiderato,negativo",
                isEnabled = true,
                isBuiltIn = true,
                priority = 3
            ),
            SpamSourceEntity(
                name = "Segnalazioni Numeri",
                urlTemplate = "https://www.segnalazioninumeri.it/numero/{number}",
                category = "Segnalazioni Utenti",
                keywordsCsv = "segnalazioni,pericoloso,molesto,truffa,spam,call center,phishing",
                isEnabled = true,
                isBuiltIn = true,
                priority = 4
            ),
            SpamSourceEntity(
                name = "Numeri Spam",
                urlTemplate = "https://www.numerispam.it/numero/{number}",
                category = "Archivio Anti-Spam",
                keywordsCsv = "spam,truffa,bloccato,molesto,telemarketing,pericoloso,energia",
                isEnabled = true,
                isBuiltIn = true,
                priority = 5
            )
        )
    }
}
