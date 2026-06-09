package com.minipai.article.feature.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.minipai.article.R
import com.minipai.article.core.network.model.SearchArticleItem
import com.minipai.article.core.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 搜索结果列表。
 * - 卡片：左缩略图 96x64dp + 右标题/描述/元数据/互动数据
 * - 滚动到底部触发 loadMore
 * - 末尾显示"加载中"或"已经到底"
 */
@Composable
fun SearchResultList(
    results: List<SearchArticleItem>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onItemClick: (SearchArticleItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    if (results.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.search_no_result),
            subtitle = "换个关键词试试",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(items = results, key = { it.id }) { item ->
            ArticleResultCard(
                item = item,
                onClick = { onItemClick(item) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 底部加载指示器
        if (isLoadingMore || hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "上滑加载更多",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (results.isNotEmpty()) {
            item {
                Text(
                    text = "— 已经到底啦 —",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * 单条结果卡片。
 * - 缩略图 96x64dp（hero 元素）
 * - 标题 2 行 + meta（时间 + 分类）+ 互动数据（浏览/评论/点赞）
 */
@Composable
private fun ArticleResultCard(
    item: SearchArticleItem,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 缩略图（hero 元素）
            val cover = item.imageUrls.firstOrNull()
            Surface(
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (cover != null) {
                    val req = remember(cover) {
                        ImageRequest.Builder(ctx)
                            .data(cover)
                            .size(144, 96)
                            .precision(Precision.INEXACT)
                            .allowHardware(false)
                            .allowRgb565(true)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Text(
                    text = item.title.ifBlank { "（无标题）" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // meta
                val meta = buildString {
                    if (item.pubTime > 0) {
                        append(formatPubTime(item.pubTime))
                    }
                    if (item.categoryName.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(item.categoryName)
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                // 互动数据
                Text(
                    text = "${formatCount(item.view)}浏览 · ${formatCount(item.reply)}评论 · ${formatCount(item.like)}点赞",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun formatPubTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    // pubTime 是秒级时间戳
    val ms = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    return dateFormat.format(Date(ms))
}

private fun formatCount(n: Int): String {
    if (n < 1000) return n.toString()
    if (n < 10_000) return String.format(Locale.getDefault(), "%.1fk", n / 1000.0)
    if (n < 100_000_000) return String.format(Locale.getDefault(), "%.1f万", n / 10_000.0)
    return String.format(Locale.getDefault(), "%.1f亿", n / 100_000_000.0)
}
