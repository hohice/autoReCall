package com.example.autoanswer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.autoanswer.calllog.CallLogDao
import com.example.autoanswer.calllog.CallLogEntry

@Database(
    entities = [BlacklistEntry::class, CallLogEntry::class],
    version = 1,
    exportSchema = false
)
abstract class BlacklistDatabase : RoomDatabase() {

    abstract fun blacklistDao(): BlacklistDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: BlacklistDatabase? = null

        fun getInstance(context: Context): BlacklistDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlacklistDatabase::class.java,
                    "auto_answer.db"
                ).build().also { INSTANCE = it }
            }
    }
}
