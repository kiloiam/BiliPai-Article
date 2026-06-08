package com.minipai.article.feature.reader

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minipai.article.ArticleApp
import com.minipai.article.core.database.ArticleReadHistory
import com.minipai.article.data.ArticleDetail
import com.minipai.article.data.ArticleReadHistoryRepository
import com.minipai.article.data.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 文章阅读 ViewModel。
 *
 * 职责：
 * - 拉取并解析专栏（`reload()`）
 * - 加载历史滚动位置 → 写入 `initialScrollIndex/Offset`
 * - 进入即 upsert 一条历史 (lastReadAt + 元数据，scroll 字段沿用旧值)
 * - 字号调整
 *
 * 滚动停止时的位置保存由 UI 在 `LaunchedEffect` 内 debounce 500ms 后回调 `onScrollChanged`。
 */
class ArticleViewModel(
    application: Application,
    val cvId: Long
) : AndroidViewModel(application) {

    private val articleRepo = ArticleRepository()
    private val historyRepo = ArticleReadHistoryRepository(
        (application as ArticleApp).database.articleReadHistoryDao()
    )

    private val _state = MutableStateFlow(ArticleUiState(isLoading = true))
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            articleRepo.loadArticle(cvId).fold(
                onSuccess = { detail ->
                    val saved = historyRepo.getByCvId(cvId)
                    val restoredIndex = saved?.scrollIndex ?: 0
                    val restoredOffset = saved?.scrollOffset ?: 0
                    _state.update {
                        it.copy(
                            isLoading = false,
                            detail = detail,
                            error = null,
                            initialScrollIndex = restoredIndex,
                            initialScrollOffset = restoredOffset,
                            // 进入就把 lastReadAt 顶到最新;scroll 字段沿用
                            // (首次进入时用恢复值,刷新不会跳走)
                        )
                    }
                    historyRepo.saveProgress(
                        ArticleReadHistory(
                            cvId = cvId,
                            title = detail.title,
                            authorName = detail.authorName,
                            coverUrl = detail.coverUrl,
                            lastReadAt = System.currentTimeMillis(),
                            scrollIndex = restoredIndex,
                            scrollOffset = restoredOffset,
                            totalBlocks = detail.blocks.size
                        )
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "loadArticle failed", e)
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
            )
        }
    }

    /** UI 在滚动停止 (debounce) 时回调,落库最新位置 */
    fun onScrollChanged(index: Int, offset: Int) {
        val d = _state.value.detail ?: return
        viewModelScope.launch {
            historyRepo.saveProgress(
                ArticleReadHistory(
                    cvId = cvId,
                    title = d.title,
                    authorName = d.authorName,
                    coverUrl = d.coverUrl,
                    lastReadAt = System.currentTimeMillis(),
                    scrollIndex = index,
                    scrollOffset = offset,
                    totalBlocks = d.blocks.size
                )
            )
        }
    }

    fun setFontSize(px: Int) {
        _state.update { it.copy(fontSize = px.coerceIn(12, 24)) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "ArticleVM"
    }
}

data class ArticleUiState(
    val isLoading: Boolean = false,
    val detail: ArticleDetail? = null,
    val error: String? = null,
    val fontSize: Int = 17,
    val initialScrollIndex: Int = 0,
    val initialScrollOffset: Int = 0
)

/**
 * 带 cvId 参数的 Factory。
 * 不用 SavedStateHandle 是因为我们只持 cvId，且不希望 ViewModel 跨进程重建。
 */
class ArticleViewModelFactory(
    private val application: Application,
    private val cvId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArticleViewModel(application, cvId) as T
    }
}
