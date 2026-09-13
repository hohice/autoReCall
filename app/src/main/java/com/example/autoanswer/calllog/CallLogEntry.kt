package com.example.autoanswer.calllog

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自动接听记录实体。
 */
@Entity(tableName = "call_log_entries")
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** 接听时生效的模式：blacklist / whitelist */
    val mode: String
)
