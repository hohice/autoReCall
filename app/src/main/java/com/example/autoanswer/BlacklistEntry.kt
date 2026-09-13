package com.example.autoanswer

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 黑名单号码实体。号码全局唯一。
 */
@Entity(
    tableName = "blacklist_entries",
    indices = [Index(value = ["number"], unique = true)]
)
data class BlacklistEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
