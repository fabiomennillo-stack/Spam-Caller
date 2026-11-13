package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey val phoneNumber: String,
    val displayName: String,
    val reason: String,
    val source: String,
    val riskScore: Int = 90,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "spam_sources")
data class SpamSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val urlTemplate: String,
    val category: String,
    val keywordsCsv: String = "spam,truffa,telemarketing,molesto,call center,pubblicita,phishing",
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val priority: Int = 10
)

@Entity(tableName = "spam_lookup_cache")
data class SpamLookupCacheEntity(
    @PrimaryKey val normalizedNumber: String,
    val riskScore: Int,
    val verdict: String,
    val reportCount: Int,
    val summary: String,
    val sourceBreakdownJson: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_backup_records")
data class LocalBackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val blockedCount: Int,
    val sourcesCount: Int,
    val cacheCount: Int,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
