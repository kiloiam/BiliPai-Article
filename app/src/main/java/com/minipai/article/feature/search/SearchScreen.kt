package com.minipai.article.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 简化版：
 * - 搜索框永远在顶部
 * - landing 态：body 显示 logo + 历史
 * - result 态：body 显示搜索结果
 * - 宽屏：搜索框居中（max 720dp），下方左历史（固定 320dp）+ 右结果（占满剩余）
 */
@Composable
fun SearchScreen(
    onOpenArticle: (cvId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val layout = rememberAppLayoutType()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (layout) {
            AppLayoutType.COMPACT -> CompactLayout(
                state = state,
                viewModel = viewModel,
                onOpenArticle = onOpenArticle
            )
            AppLayoutType.MEDIUM, AppLayoutType.EXPANDED -> WideLayout(
                state = state,
                viewModel = viewModel,
                onOpenArticle = onOpenArticle
            )
        }
    }
}

// ============== 单栏布局（手机竖屏） ==============

@Composable
private fun CompactLayout(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenArticle: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
    ) {
        SearchBarTop(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
        Box(modifier = Modifier.fillMaxSize()) {
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

@Composable
private fun LandingBody(
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部：logo + 提示（居中，固定区域）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Logo()
            QuickTip()
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        // 下方：历史列表（撑满剩余空间）
        SearchHistoryPanel(
            history = state.history,
            onHistoryClick = viewModel::onHistoryClick,
            onHistoryLongClick = viewModel::deleteHistory,
            onClearAll = viewModel::clearHistory,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ============== 宽屏布局（手机横屏 / 平板） ==============

@Composable
private fun WideLayout(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenArticle: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
    ) {
        // 顶部：搜索框居中（max 720dp）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            SearchBarTop(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp)
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        // 主体：左历史 320dp 固定 + 右结果占满剩余
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                SearchHistoryPanel(
                    history = state.history,
                    onHistoryClick = viewModel::onHistoryClick,
                    onHistoryLongClick = viewModel::deleteHistory,
                    onClearAll = viewModel::clearHistory
                )
            }
            VerticalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (state.isLanding) {
                    WideLandingHint()
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

@Composable
private fun WideLandingHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "输入关键词开始搜索",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============== 复用组件 ==============

@Composable
private fun SearchBarTop(
    state: SearchUiState,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    BiliSearchBar(
        query = state.query,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = { viewModel.onSubmit(state.query) },
        onClear = { viewModel.onQueryChange("") },
        expanded = true,
        modifier = modifier
    )
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

@Composable
private fun Logo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83D\uDCD6",
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            text = "BiliPai 专栏",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickTip() {
    Text(
        text = "搜索 B 站图文专栏",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
