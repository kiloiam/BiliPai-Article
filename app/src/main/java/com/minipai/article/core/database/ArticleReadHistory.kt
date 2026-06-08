package com.minipai.article.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读历史记录。
 *
 * 每次进入文章页就 upsert 一条 (OnConflictStrategy.REPLACE);
 * 滚动停止后 (debounce 500ms) 更新 scrollIndex/scrollOffset;
 * 上次进入的滚动位置被 restore 给 `LazyListState` 的 initial 值。
 *
 * `totalBlocks` 用来粗略估算阅读进度百分比 "上次读到 32%"。
 */
@Entity(tableName = "article_read_history")
data class ArticleReadHistory(
    @PrimaryKey
    val cvId: Long,
    val title: String,
    val authorName: String,
    val coverUrl: String? = null,
    val lastReadAt: Long,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val totalBlocks: Int = 0
)
