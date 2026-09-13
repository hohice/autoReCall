package com.example.autoanswer.calllog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_log_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CallLogEntry>>

    @Insert
    suspend fun insert(entry: CallLogEntry): Long

    @Query("DELETE FROM call_log_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_log_entries")
    suspend fun clear()
}
