package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamcallerDao {

    // Blocked Numbers
    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun observeBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    suspend fun getAllBlockedNumbersOnce(): List<BlockedNumberEntity>

    @Query("SELECT * FROM blocked_numbers WHERE phoneNumber = :normalized LIMIT 1")
    suspend fun getBlockedNumber(normalized: String): BlockedNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedNumber(entity: BlockedNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedNumbers(entities: List<BlockedNumberEntity>)

    @Query("DELETE FROM blocked_numbers WHERE phoneNumber = :normalized")
    suspend fun deleteBlockedNumber(normalized: String)

    // Spam Online Sources
    @Query("SELECT * FROM spam_sources ORDER BY priority ASC, id ASC")
    fun observeSpamSources(): Flow<List<SpamSourceEntity>>

    @Query("SELECT * FROM spam_sources WHERE isEnabled = 1 ORDER BY priority ASC, id ASC")
    suspend fun getEnabledSpamSourcesOnce(): List<SpamSourceEntity>

    @Query("SELECT * FROM spam_sources ORDER BY priority ASC, id ASC")
    suspend fun getAllSpamSourcesOnce(): List<SpamSourceEntity>

    @Query("SELECT COUNT(*) FROM spam_sources")
    suspend fun countSpamSources(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamSource(entity: SpamSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamSources(entities: List<SpamSourceEntity>)

    @Update
    suspend fun updateSpamSource(entity: SpamSourceEntity)

    @Query("DELETE FROM spam_sources WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomSpamSource(id: Long)

    // Lookup Cache
    @Query("SELECT * FROM spam_lookup_cache ORDER BY fetchedAt DESC")
    fun observeLookupCache(): Flow<List<SpamLookupCacheEntity>>

    @Query("SELECT * FROM spam_lookup_cache ORDER BY fetchedAt DESC")
    suspend fun getAllLookupCacheOnce(): List<SpamLookupCacheEntity>

    @Query("SELECT * FROM spam_lookup_cache WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun getLookupCache(normalized: String): SpamLookupCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLookupCache(entity: SpamLookupCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLookupCaches(entities: List<SpamLookupCacheEntity>)

    // Local Backup Records
    @Query("SELECT * FROM local_backup_records ORDER BY createdAt DESC")
    fun observeBackupRecords(): Flow<List<LocalBackupRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(entity: LocalBackupRecordEntity): Long

    @Query("DELETE FROM local_backup_records WHERE id = :id")
    suspend fun deleteBackupRecord(id: Long)
}
