package com.minipai.article.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipai.article.R
import com.minipai.article.core.ui.adaptive.AppLayoutType
import com.minipai.article.core.ui.adaptive.rememberAppLayoutType
import com.minipai.article.core.ui.components.BiliSearchBar
import com.minipai.article.core.ui.theme.BiliPink

/**
 * 搜索页主 Composable。
 *
 * 关键设计：
 * 1. landing 态：搜索框**垂直水平居中**，logo 在上、QuickTip + 历史在下
 * 2. result 态：搜索框动画到顶，下方展示结果列表
 * 3. 用 `AnimatedContent` 切换两套布局，避免 layout 竞争导致 0 高度崩溃
 * 4. 宽屏：搜索框视觉居中（max 640dp），下方左右分栏（历史 40% + 结果 60%）
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
                onOpenArticle = onOpenArticle
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
    AnimatedContent(
        targetState = state.isLanding,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) +
                slideInVertically(animationSpec = tween(220)) { if (targetState) -it / 8 else it / 8 })
                .togetherWith(
                    fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(animationSpec = tween(180)) { if (targetState) it / 8 else -it / 8 }
                )
        },
        label = "searchMode"
    ) { isLanding ->
        if (isLanding) {
            LandingLayout(state = state, viewModel = viewModel)
        } else {
            ResultLayout(state = state, viewModel = viewModel, onOpenArticle = onOpenArticle)
        }
    }
}

/**
 * Landing 布局：搜索框视觉居中，logo 在上方，QuickTip + 历史在下方。
 */
@Composable
private fun LandingLayout(
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
    ) {
        // 顶部 Logo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Logo()
        }

        // 居中搜索框
        BiliSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = { viewModel.onSubmit(state.query) },
            onClear = { viewModel.onQueryChange("") },
            expanded = false,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        )

        // 底部：QuickTip + 历史面板
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            QuickTip()
            Spacer(Modifier.height(16.dp))
            SearchHistoryPanel(
                history = state.history,
                onHistoryClick = viewModel::onHistoryClick,
                onHistoryLongClick = viewModel::deleteHistory,
                onClearAll = viewModel::clearHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            )
        }
    }
}

/**
 * Result 布局：搜索框顶部 + 下方结果区。
 */
@Composable
private fun ResultLayout(
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
        BiliSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = { viewModel.onSubmit(state.query) },
            onClear = { viewModel.onQueryChange("") },
            expanded = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
        Box(modifier = Modifier.fillMaxSize()) {
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
                text = "\uD83D\uDCD6",
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
        text = "\uD83D\uDCA1 试试搜：机械键盘、独立游戏、考研",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    )
}

// ============== 多栏布局（手机横屏 / 平板） ==============

@Composable
private fun MultiColumnSearchLayout(
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
        // 顶部：搜索框视觉居中（最大 640dp）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BiliSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onSubmit = { viewModel.onSubmit(state.query) },
                onClear = { viewModel.onQueryChange("") },
                expanded = true,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 主体：左历史 40% + 右结果 60%
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .weight(0.4f)
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
            Box(modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
            ) {
                if (state.isSearching) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = BiliPink,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (state.query.isNotBlank() || state.results.isNotEmpty()) {
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
}
