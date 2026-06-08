package com.minipai.article.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleReadHistoryDao {
    @Query("SELECT * FROM article_read_history ORDER BY lastReadAt DESC LIMIT 100")
    fun observeAll(): Flow<List<ArticleReadHistory>>

    @Query("SELECT * FROM article_read_history WHERE cvId = :cvId LIMIT 1")
    suspend fun getByCvId(cvId: Long): ArticleReadHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ArticleReadHistory)

    @Query("DELETE FROM article_read_history WHERE cvId = :cvId")
    suspend fun delete(cvId: Long)

    @Query("DELETE FROM article_read_history")
    suspend fun clearAll()
}
