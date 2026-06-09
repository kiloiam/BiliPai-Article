package com.minipai.article.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minipai.article.R
import com.minipai.article.core.database.SearchHistory
import com.minipai.article.core.ui.components.BiliHistoryCard
import com.minipai.article.core.ui.components.EmptyState
import com.minipai.article.core.util.HistoryGroup
import com.minipai.article.core.util.groupByTime

/**
 * 历史面板（卡片化重写版）。
 * - LazyColumn + 按今天/昨天/更早分组
 * - 每条用 BiliHistoryCard（16dp 圆角卡片）
 * - 顶部"清空"按钮弹出确认菜单
 * - 长按单条 → 弹删除菜单
 */
@Composable
fun SearchHistoryPanel(
    history: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onHistoryLongClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuFor by remember { mutableStateOf<String?>(null) }
    var showClearMenu by remember { mutableStateOf(false) }

    if (history.isEmpty()) {
        EmptyState(
            title = "还没有搜索记录",
            subtitle = "试试搜点什么吧",
            modifier = modifier
        )
        return
    }

    val grouped = history.groupByTime()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部操作栏
        item(key = "header") {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { showClearMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.history_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showClearMenu,
                        onDismissRequest = { showClearMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_clear)) },
                            onClick = {
                                showClearMenu = false
                                onClearAll()
                            }
                        )
                    }
                }
            }
        }

        grouped.forEach { (group, items) ->
            item(key = "group_${group.name}") {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(items = items, key = { it.keyword }) { item ->
                BiliHistoryCard(
                    history = item,
                    onClick = { onHistoryClick(item.keyword) },
                    onLongClick = { menuFor = item.keyword }
                )
                androidx.compose.foundation.layout.Box {
                    DropdownMenu(
                        expanded = menuFor == item.keyword,
                        onDismissRequest = { menuFor = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_delete)) },
                            onClick = {
                                menuFor = null
                                onHistoryLongClick(item.keyword)
                            }
                        )
                    }
                }
            }
        }
    }
}
