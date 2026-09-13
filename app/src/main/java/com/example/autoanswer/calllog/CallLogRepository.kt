package com.example.autoanswer.calllog

import kotlinx.coroutines.flow.Flow

/**
 * 接听记录数据层。
 */
class CallLogRepository(private val dao: CallLogDao) {

    fun getAll(): Flow<List<CallLogEntry>> = dao.getAll()

    suspend fun addEntry(entry: CallLogEntry) {
        dao.insert(entry)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clear() = dao.clear()
}
