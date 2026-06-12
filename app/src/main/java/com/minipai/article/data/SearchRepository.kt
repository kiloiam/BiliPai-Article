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
 * 搜索仓库（对齐 BiliPai 原版 SearchRepository 行为）。
 *
 * 关键差异：
 * - WBI 密钥通过 WbiKeyManager 24h 缓存 + SP 持久化（减少 nav 请求频率，降低 352 风险）
 * - 发送 WBI 风控指纹 dm_img_* 字段（冷启动会话不足时必要）
 * - platform=pc
 * - 失败回退到无签名参数
 * - 明确处理 -352 风控错误码
 */
class SearchRepository(
    private val historyDao: SearchHistoryDao
) {

    // ============== 搜索 ==============

    suspend fun searchArticle(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<SearchArticleData> = withContext(Dispatchers.IO) {
        try {
            // 等待会话预热完成（避免冷启动直搜被风控），最多等 5 秒
            kotlinx.coroutines.withTimeoutOrNull(5000L) {
                NetworkModule.warmupReady.await()
            }
            val params = mapOf(
                "keyword" to keyword,
                "search_type" to "article",
                "page" to page.toString(),
                "page_size" to pageSize.toString(),
                "platform" to "pc",
                "web_location" to "1430654",
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
     * 每次搜索实时拉取 WBI 密钥并签名（对齐 BiliPai 原版行为）。
     * 不做缓存，避免密钥过期引发静默空结果。
     */
    private suspend fun signWithWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
            if (keys != null) {
                WbiUtils.sign(params, keys.first, keys.second, includeRiskFingerprint = true)
            } else {
                // fallback: 实时拉取 WBI 密钥
                val navResp = NetworkModule.navApi.getNavInfo()
                val wbiImg = navResp.data?.wbi_img
                val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
                val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
                if (imgKey.isNotEmpty() && subKey.isNotEmpty()) {
                    WbiUtils.sign(params, imgKey, subKey, includeRiskFingerprint = true)
                } else {
                    params
                }
            }
        } catch (e: Exception) {
            params
        }
    }

    private fun createSearchError(code: Int, message: String): Exception {
        val readable = when (code) {
            -412 -> "搜索请求被拦截，请稍后重试"
            -400 -> "搜索参数错误"
            -404 -> "搜索接口不存在"
            -352 -> "请求频率过高（B站风控），请稍后重试"
            -1200 -> "搜索类型不存在或参数被降级过滤"
            else -> message.ifBlank { "搜索失败 ($code)" }
        }
        return Exception(readable)
    }
}
