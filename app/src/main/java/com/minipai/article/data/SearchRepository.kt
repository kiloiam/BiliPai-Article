package com.minipai.article.data

import com.minipai.article.core.database.SearchHistory
import com.minipai.article.core.database.SearchHistoryDao
import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.WbiKeyManager
import com.minipai.article.core.network.WbiUtils
import com.minipai.article.core.network.model.SearchArticleData
import com.minipai.article.core.network.model.SearchArticleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索仓库。
 * 职责：
 * 1) 调用 WbiKeyManager 拿到 img/sub key，调用 WbiUtils.sign 加签
 * 2) 通过 SearchApi.searchArticle 拿数据
 * 3) 把数据类做 cleanupFields（去 <em> 标签、补全 https://）
 * 4) 把 B 站错误码翻译成可读中文
 */
class SearchRepository(
    private val historyDao: SearchHistoryDao
) {

    // ============== 搜索 ==============

    /**
     * 搜索专栏。
     * @param keyword 关键词
     * @param page 从 1 开始
     * @param pageSize 默认 20
     */
    suspend fun searchArticle(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<SearchArticleData> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "keyword" to keyword,
                "search_type" to "article",
                "page" to page.toString(),
                "page_size" to pageSize.toString(),
                "platform" to "pc",
                "web_location" to "1430654",
                "order" to "totalrank"
            )
            val signed = signWithWbi(params)
            val response = NetworkModule.searchApi.searchArticle(signed)

            if (response.code != 0) {
                return@withContext Result.failure(
                    createSearchError(response.code, response.message)
                )
            }

            val data = response.data ?: SearchArticleData()
            val cleaned = data.copy(
                result = data.result?.map { it.cleanupFields() }
            )
            Result.success(cleaned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============== 历史 ==============

    fun observeHistory() = historyDao.getAll()

    /**
     * 记录一次搜索。
     * 行为：同名 keyword 自增 count + 更新时间戳；新 keyword 插入新行。
     */
    suspend fun recordSearch(keyword: String) = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@withContext
        runCatching {
            historyDao.insert(SearchHistory(keyword = trimmed, timestamp = System.currentTimeMillis(), searchCount = 1))
            historyDao.incrementCount(trimmed)
        }
    }

    suspend fun deleteHistory(keyword: String) = withContext(Dispatchers.IO) {
        runCatching {
            historyDao.delete(SearchHistory(keyword = keyword, timestamp = 0L, searchCount = 0))
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        runCatching { historyDao.clearAll() }
    }

    // ============== 内部 ==============

    /**
     * 拉 WBI 密钥 + 签名。失败时回退到无签参数（极少数情况下 B 站会放行，但通常 -352）。
     */
    private suspend fun signWithWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val keysResult = WbiKeyManager.getWbiKeys()
            val (imgKey, subKey) = keysResult.getOrNull() ?: ("" to "")
            if (imgKey.isNotEmpty() && subKey.isNotEmpty()) {
                WbiUtils.sign(params, imgKey, subKey)
            } else {
                params
            }
        } catch (e: Exception) {
            params
        }
    }

    private fun createSearchError(code: Int, message: String): Exception {
        val readable = when (code) {
            -352 -> "风控校验失败，请稍后重试"
            -412 -> "搜索请求被拦截，请稍后重试"
            -400 -> "搜索参数错误"
            -404 -> "搜索接口不存在"
            -1200 -> "搜索类型不存在或参数被降级过滤"
            else -> message.ifBlank { "搜索失败 ($code)" }
        }
        return Exception(readable)
    }
}
