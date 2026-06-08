package com.minipai.article.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    /** 拉取最近 30 条历史（按时间倒序），Flow 驱动 UI 自动更新 */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getAll(): Flow<List<SearchHistory>>

    /**
     * 插入或替换。同 keyword 第二次搜索会触发 REPLACE，更新 timestamp。
     * 注：searchCount 不会自动 +1，需要在 ViewModel 里先 incrementCount 再 insert。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory)

    /**
     * 自增同名搜索次数。
     * 行为：把对应 keyword 的 searchCount 加 1，timestamp 更新为当前时间。
     * 不存在的 keyword 不创建新行（让 ViewModel 先调 insert 初始化）。
     */
    @Query("UPDATE search_history SET searchCount = searchCount + 1, timestamp = :now WHERE keyword = :keyword")
    suspend fun incrementCount(keyword: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(history: SearchHistory)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
