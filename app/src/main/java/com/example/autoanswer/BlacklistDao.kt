package com.example.autoanswer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BlacklistEntry>>

    @Query("SELECT COUNT(*) FROM blacklist_entries WHERE number = :number")
    suspend fun countByNumber(number: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: BlacklistEntry): Long

    @Delete
    suspend fun delete(entry: BlacklistEntry)

    @Query("DELETE FROM blacklist_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
