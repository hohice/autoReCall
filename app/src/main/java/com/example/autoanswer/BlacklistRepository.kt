package com.example.autoanswer

import kotlinx.coroutines.flow.Flow

/**
 * 黑名单数据层。
 */
class BlacklistRepository(private val dao: BlacklistDao) {

    fun getAll(): Flow<List<BlacklistEntry>> = dao.getAll()

    suspend fun isBlacklisted(number: String): Boolean =
        dao.countByNumber(number) > 0

    suspend fun addNumber(number: String, note: String? = null) {
        dao.insert(BlacklistEntry(number = number.trim(), note = note))
    }

    suspend fun delete(entry: BlacklistEntry) = dao.delete(entry)
}
