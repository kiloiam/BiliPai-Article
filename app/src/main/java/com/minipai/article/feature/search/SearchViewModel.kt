package com.minipai.article.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipai.article.data.SearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索页 ViewModel。
 * 灵魂逻辑（从 BiliPai 照搬，去掉搜索建议部分）：
 * 1) 300ms 防抖：用户连续输入不立刻发请求
 * 2) sessionId 防竞态：多次快速输入只采纳最后一次结果
 * 3) 历史 Flow 驱动：Room 变更自动刷新 UI
 */
@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SearchRepository(
        (application as com.minipai.article.ArticleApp).database.searchHistoryDao()
    )

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** 防抖/防竞态句柄 */
    private var searchJob: Job? = null
    private var activeSessionId: Long = 0L

    init {
        observeHistory()
    }

    // ============== 公开 API ==============

    /**
     * 用户输入框变化时调用。300ms 防抖。
     * - 清空 → 切回 landing 模式
     * - 有内容 → 延迟 300ms 触发搜索
     */
    fun onQueryChange(keyword: String) {
        _uiState.update { it.copy(query = keyword, error = null) }

        searchJob?.cancel()
        if (keyword.isBlank()) {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    isSearching = false,
                    currentPage = 1,
                    totalPages = 1,
                    hasMore = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            executeSearch(keyword, page = 1, append = false)
        }
    }

    /**
     * 用户按回车 / 点击搜索按钮。立即触发（不等 300ms）。
     */
    fun onSubmit(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return

        searchJob?.cancel()
        viewModelScope.launch {
            // 同步写历史（不等网络结果，先记录这次搜索行为）
            repository.recordSearch(trimmed)
            executeSearch(trimmed, page = 1, append = false)
        }
    }

    /**
     * 用户点击历史卡片：重新搜索，不写入历史（避免重复）。
     */
    fun onHistoryClick(keyword: String) {
        _uiState.update { it.copy(query = keyword, error = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(keyword, page = 1, append = false)
        }
    }

    /**
     * 滚动到列表底部：加载下一页。
     */
    fun loadMore() {
        val s = _uiState.value
        if (!s.hasMore || s.isLoadingMore || s.isSearching) return
        searchJob = viewModelScope.launch {
            executeSearch(s.query, page = s.currentPage + 1, append = true)
        }
    }

    fun deleteHistory(keyword: String) {
        viewModelScope.launch { repository.deleteHistory(keyword) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ============== 内部 ==============

    private fun observeHistory() {
        viewModelScope.launch {
            repository.observeHistory()
                .catch { /* 数据库异常时保持空列表 */ }
                .collect { list ->
                    _uiState.update { it.copy(history = list) }
                }
        }
    }

    /**
     * 核心：执行一次搜索。
     * - 自增 sessionId，过期结果丢弃
     * - append=true 走 loadMore 路径，false 走替换路径
     */
    private suspend fun executeSearch(keyword: String, page: Int, append: Boolean) {
        val sessionId = activeSessionId + 1
        activeSessionId = sessionId

        if (append) {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
        } else {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    results = emptyList(),
                    currentPage = 1,
                    totalPages = 1,
                    hasMore = false,
                    error = null
                )
            }
        }

        val result = repository.searchArticle(keyword, page = page)

        // 防竞态：如果在请求过程中又触发了新的搜索，丢弃本次结果
        if (sessionId != activeSessionId) return

        result.fold(
            onSuccess = { data ->
                val newList = if (append) _uiState.value.results + data.result.orEmpty()
                              else data.result.orEmpty()
                val totalPages = data.numPages.takeIf { it > 0 } ?: 1
                val hasMore = page < totalPages
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        results = newList,
                        currentPage = data.page,
                        totalPages = totalPages,
                        hasMore = hasMore,
                        error = null
                    )
                }
            },
            onFailure = { e ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        error = e.message ?: "搜索失败"
                    )
                }
            }
        )
    }
}
