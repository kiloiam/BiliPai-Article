package com.minipai.article.feature.search

import com.minipai.article.core.database.SearchHistory
import com.minipai.article.core.network.model.SearchArticleItem

/**
 * 搜索页 UI 状态。
 * - results 为空 + query 为空 → 显示历史/空态
 * - results 为空 + query 非空 → 显示"没有找到相关专栏"
 * - isSearching=true → 显示骨架屏或进度条
 */
data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchArticleItem> = emptyList(),
    val history: List<SearchHistory> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
) {
    /** 是否处于"展示历史"模式（未输入关键词） */
    val isLanding: Boolean get() = query.isBlank() && results.isEmpty() && !isSearching

    /** 是否处于"展示结果"模式（已输入且有结果） */
    val isResultMode: Boolean get() = results.isNotEmpty()
}
