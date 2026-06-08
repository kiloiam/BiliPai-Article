package com.minipai.article.core.util

import com.minipai.article.core.database.SearchHistory
import java.util.Calendar

/**
 * 把搜索历史按"今天 / 昨天 / 更早"分组。
 * 用于历史面板的分组标题。
 */
enum class HistoryGroup(val label: String) {
    TODAY("今天"),
    YESTERDAY("昨天"),
    EARLIER("更早")
}

fun List<SearchHistory>.groupByTime(): LinkedHashMap<HistoryGroup, List<SearchHistory>> {
    val cal = Calendar.getInstance()
    val todayStart = (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

    val today = mutableListOf<SearchHistory>()
    val yesterday = mutableListOf<SearchHistory>()
    val earlier = mutableListOf<SearchHistory>()

    for (item in this) {
        when {
            item.timestamp >= todayStart -> today.add(item)
            item.timestamp >= yesterdayStart -> yesterday.add(item)
            else -> earlier.add(item)
        }
    }

    return linkedMapOf(
        HistoryGroup.TODAY to today,
        HistoryGroup.YESTERDAY to yesterday,
        HistoryGroup.EARLIER to earlier
    ).filterValues { it.isNotEmpty() }
        as LinkedHashMap<HistoryGroup, List<SearchHistory>>
}
