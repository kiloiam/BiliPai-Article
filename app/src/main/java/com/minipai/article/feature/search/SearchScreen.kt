package com.minipai.article.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipai.article.core.ui.adaptive.AppLayoutType
import com.minipai.article.core.ui.adaptive.rememberAppLayoutType
import com.minipai.article.core.ui.components.BiliSearchBar
import com.minipai.article.core.ui.theme.BiliPink

/**
 * 搜索页。
 *
 * 单列设计（横竖屏同一套结构）：
 * - 搜索框永远在顶部
 * - landing 态：body 显示"BiliPai 专栏"标题 + 历史
 * - result 态：body 显示搜索结果
 * - 宽屏唯一区别：搜索框 + 内容区都受 widthIn(max=720dp) 限制并居中
 */
@Composable
fun SearchScreen(
    onOpenArticle: (cvId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val layout = rememberAppLayoutType()
    val maxContentWidth = when (layout) {
        AppLayoutType.COMPACT -> Modifier.fillMaxWidth()
        else -> Modifier.fillMaxWidth().widthIn(max = 720.dp)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .statusBarsPadding()
        ) {
            // 顶部搜索框（宽屏时居中限宽）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BiliSearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = { viewModel.onSubmit(state.query) },
                    onClear = { viewModel.onQueryChange("") },
                    expanded = true,
                    modifier = maxContentWidth
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            // body 也用同样的限宽居中
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = maxContentWidth.fillMaxSize()) {
                    if (state.isLanding) {
                        LandingBody(state = state, viewModel = viewModel)
                    } else {
                        ResultBody(
                            state = state,
                            onItemClick = { onOpenArticle(it.id) },
                            onLoadMore = viewModel::loadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingBody(
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题（无 emoji，纯文字）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BiliPai",
                style = MaterialTheme.typography.headlineMedium,
                color = BiliPink,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "搜索 B 站图文专栏",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        // 历史列表（撑满剩余）
        SearchHistoryPanel(
            history = state.history,
            onHistoryClick = viewModel::onHistoryClick,
            onHistoryLongClick = viewModel::deleteHistory,
            onClearAll = viewModel::clearHistory,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ResultBody(
    state: SearchUiState,
    onItemClick: (com.minipai.article.core.network.model.SearchArticleItem) -> Unit,
    onLoadMore: () -> Unit
) {
    if (state.isSearching) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = BiliPink,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    } else {
        SearchResultList(
            results = state.results,
            isLoadingMore = state.isLoadingMore,
            hasMore = state.hasMore,
            onItemClick = onItemClick,
            onLoadMore = onLoadMore
        )
    }
}
