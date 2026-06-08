package com.minipai.article.data

import com.minipai.article.core.database.ArticleReadHistory
import com.minipai.article.core.database.ArticleReadHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ArticleReadHistoryRepository(private val dao: ArticleReadHistoryDao) {

    fun observeHistory(): Flow<List<ArticleReadHistory>> = dao.observeAll()

    suspend fun getByCvId(cvId: Long): ArticleReadHistory? = withContext(Dispatchers.IO) {
        dao.getByCvId(cvId)
    }

    suspend fun saveProgress(record: ArticleReadHistory) = withContext(Dispatchers.IO) {
        runCatching { dao.upsert(record) }
    }

    suspend fun delete(cvId: Long) = withContext(Dispatchers.IO) {
        runCatching { dao.delete(cvId) }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching { dao.clearAll() }
    }
}
