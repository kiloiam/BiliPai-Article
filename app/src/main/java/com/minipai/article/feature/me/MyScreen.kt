package com.minipai.article.feature.me

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipai.article.core.database.ArticleReadHistory
import com.minipai.article.core.ui.components.EmptyState

/**
 * 「我的」页 — 阅读历史列表。
 * - LazyColumn，封面 96x64 + 右标题/作者/「上次读到 32% · 2 小时前」
 * - 长按单条弹删除菜单
 * - 空态：EmptyState("📚", "还没读过文章", "去搜索看看")
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyScreen(
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = viewModel()
) {
    val items by viewModel.history.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (items.isEmpty()) {
            EmptyState(
                emoji = "📚",
                title = "还没读过文章",
                subtitle = "去搜索看看",
                modifier = Modifier.fillMaxSize()
            )
            return@Surface
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Text(
                    text = "阅读历史",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(items = items, key = { it.cvId }) { record ->
                ReadHistoryCard(
                    record = record,
                    onClick = { onOpenArticle(record.cvId) },
                    onDelete = { viewModel.delete(record.cvId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadHistoryCard(
    record: ArticleReadHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuFor by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuFor = true }
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面 96x64
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!record.coverUrl.isNullOrBlank()) {
                    val req = remember(record.coverUrl) {
                        ImageRequest.Builder(ctx).data(record.coverUrl).crossfade(true).build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.title.ifBlank { "专栏 ${record.cvId}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (record.authorName.isNotBlank()) {
                    Text(
                        text = record.authorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val progress = computeProgressPercent(record)
                val timeText = formatRelativeTime(record.lastReadAt)
                val meta = listOfNotNull(
                    progress?.let { "上次读到 $it%" },
                    timeText.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box {
            DropdownMenu(
                expanded = menuFor,
                onDismissRequest = { menuFor = false }
            ) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        menuFor = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun computeProgressPercent(record: ArticleReadHistory): Int? {
    if (record.totalBlocks <= 0) return null
    val pct = (record.scrollIndex.toFloat() / record.totalBlocks.toFloat() * 100f).toInt()
    return pct.coerceIn(0, 100)
}

private fun formatRelativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = (now - epochMillis).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 7 * day -> "${diff / day} 天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(epochMillis))
        }
    }
}
