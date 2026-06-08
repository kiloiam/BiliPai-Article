package com.minipai.article.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipai.article.R
import com.minipai.article.core.ui.adaptive.AppLayoutType
import com.minipai.article.core.ui.adaptive.rememberAppLayoutType
import com.minipai.article.core.ui.components.BiliSearchBar
import com.minipai.article.core.ui.components.EmptyState
import com.minipai.article.core.ui.theme.BiliPink

/**
 * 搜索页主 Composable。
 *
 * 关键设计：
 * 1. 搜索框初始态垂直水平居中（landing）
 * 2. 用户输入后，搜索框动画过渡到顶部（expanded）
 * 3. 自适应：手机竖屏单栏 / 手机横屏+小平板左右分栏 / 平板三栏
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
            AppLayoutType.COMPACT -> CompactSearchLayout(
                state = state,
                viewModel = viewModel,
                onOpenArticle = onOpenArticle
            )
            AppLayoutType.MEDIUM, AppLayoutType.EXPANDED -> MultiColumnSearchLayout(
                state = state,
                viewModel = viewModel,
                onOpenArticle = onOpenArticle,
                showResultColumn = layout == AppLayoutType.EXPANDED
            )
        }
    }
}

// ============== 单栏布局（手机竖屏） ==============

@Composable
private fun CompactSearchLayout(
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
        // 顶部搜索框区
        AnimatedSearchHeader(state = state, viewModel = viewModel)

        // 主体内容：历史 / 结果 / 空态
        // Column 内两个 AnimatedVisibility 通过 weight=1f 平分空间，可见的那一个占满
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            AnimatedVisibility(
                visible = state.isLanding,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                SearchHistoryPanel(
                    history = state.history,
                    onHistoryClick = viewModel::onHistoryClick,
                    onHistoryLongClick = viewModel::deleteHistory,
                    onClearAll = viewModel::clearHistory
                )
            }
            AnimatedVisibility(
                visible = !state.isLanding,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
                modifier = Modifier.fillMaxSize()
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
                        onItemClick = { item -> onOpenArticle(item.id) },
                        onLoadMore = viewModel::loadMore
                    )
                }
            }
        }
    }
}

/**
 * 搜索框在 landing 态居中，expanded 态顶部。
 */
@Composable
private fun AnimatedSearchHeader(
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    val expanded = state.isResultMode || state.isSearching
    val topPadding by animateDpAsState(
        targetValue = if (expanded) 12.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchHeaderTop"
    )
    val bottomPadding by animateDpAsState(
        targetValue = if (expanded) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchHeaderBottom"
    )

    // landing 态：占据整个屏幕高度，搜索框居中
    if (!expanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Logo()
                BiliSearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = { viewModel.onSubmit(state.query) },
                    onClear = { viewModel.onQueryChange("") },
                    expanded = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                QuickTip()
            }
        }
    } else {
        // expanded 态：搜索框在顶部
        BiliSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = { viewModel.onSubmit(state.query) },
            onClear = { viewModel.onQueryChange("") },
            expanded = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding, bottom = bottomPadding, start = 8.dp, end = 8.dp)
                .statusBarsPadding()
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
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📖",
                style = MaterialTheme.typography.displayMedium
            )
        }
        Text(
            text = "BiliPai 专栏",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Text(
            text = "搜索 B 站图文专栏",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickTip() {
    Text(
        text = "💡 试试搜：机械键盘、独立游戏、考研",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

// ============== 多栏布局（手机横屏 / 平板） ==============

@Composable
private fun MultiColumnSearchLayout(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenArticle: (Long) -> Unit,
    showResultColumn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 左侧：搜索框 + 历史（30%）
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            BiliSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onSubmit = { viewModel.onSubmit(state.query) },
                onClear = { viewModel.onQueryChange("") },
                expanded = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            SearchHistoryPanel(
                history = state.history,
                onHistoryClick = viewModel::onHistoryClick,
                onHistoryLongClick = viewModel::deleteHistory,
                onClearAll = viewModel::clearHistory
            )
        }
        // 分割线
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        // 右侧：结果列表（70%）
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (state.isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = BiliPink,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (state.query.isNotBlank()) {
                SearchResultList(
                    results = state.results,
                    isLoadingMore = state.isLoadingMore,
                    hasMore = state.hasMore,
                    onItemClick = { item -> onOpenArticle(item.id) },
                    onLoadMore = viewModel::loadMore
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "输入关键词开始搜索",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
