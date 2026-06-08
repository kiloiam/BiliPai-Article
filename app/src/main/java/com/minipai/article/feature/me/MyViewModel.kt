package com.minipai.article.feature.me

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipai.article.ArticleApp
import com.minipai.article.core.database.ArticleReadHistory
import com.minipai.article.data.ArticleReadHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 「我的」页 ViewModel。直接 observe 阅读历史的 Flow。
 */
class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArticleReadHistoryRepository(
        (application as ArticleApp).database.articleReadHistoryDao()
    )

    val history: StateFlow<List<ArticleReadHistory>> = repository.observeHistory()
        .catch { /* 数据库异常时保持空列表 */ emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun delete(cvId: Long) {
        viewModelScope.launch { repository.delete(cvId) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
