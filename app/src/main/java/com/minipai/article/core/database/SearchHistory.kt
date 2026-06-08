package com.minipai.article.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 搜索历史实体。
 * - keyword 为主键 → 重复搜索时自动去重（@Insert REPLACE 会更新 timestamp）
 * - searchCount 累加同名搜索次数，UI 上展示徽章
 */
@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis(),
    val searchCount: Int = 1
)
