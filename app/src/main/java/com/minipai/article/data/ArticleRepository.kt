package com.minipai.article.data

import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.WbiKeyManager
import com.minipai.article.core.network.WbiUtils
import com.minipai.article.core.network.normalizeSearchImageUrl
import com.minipai.article.core.network.model.Stats
import com.minipai.article.feature.reader.HtmlToBlocks
import com.minipai.article.feature.reader.ParagraphsToBlocks
import com.minipai.article.feature.reader.model.ArticleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 拉取并解析专栏详情（对齐 BiliPai 原版行为）。
 *
 * - 现在也走 WBI 签名（原版 getArticleDetail 调用了 signWithWbi）
 * - 传 gaia_source=main_web + web_location=333.976
 */
class ArticleRepository {

    suspend fun loadArticle(cvId: Long): Result<ArticleDetail> = withContext(Dispatchers.IO) {
        runCatching {
            // 等待会话预热完成 + WBI 密钥就绪，最多等 5 秒
            kotlinx.coroutines.withTimeoutOrNull(5000L) {
                NetworkModule.warmupReady.await()
            }
            val params = signWithWbi(
                mapOf(
                    "id" to cvId.toString(),
                    "gaia_source" to "main_web",
                    "web_location" to "333.976"
                )
            )
            val resp = NetworkModule.articleApi.getArticleView(params)
            if (resp.code != 0) {
                error(articleErrorMessage(resp.code, resp.message))
            }
            val d = resp.data ?: error("专栏数据为空")

            val blocks: List<ArticleBlock> = when {
                !d.opus?.content?.paragraphs.isNullOrEmpty() ->
                    ParagraphsToBlocks.convert(d.opus!!.content!!.paragraphs)
                !d.opus?.h5Content.isNullOrBlank() ->
                    HtmlToBlocks.convert(d.opus!!.h5Content)
                d.content.isNotBlank() ->
                    HtmlToBlocks.convert(d.content)
                else -> emptyList()
            }

            ArticleDetail(
                cvId = d.id.takeIf { it > 0 } ?: cvId,
                title = d.title.ifBlank { "专栏 $cvId" },
                authorName = d.author.name,
                publishTime = d.publishTime,
                stats = d.stats,
                blocks = blocks,
                coverUrl = normalizeSearchImageUrl(
                    d.bannerUrl.takeIf { it.isNotBlank() } ?: d.imageUrls.firstOrNull().orEmpty()
                ).ifBlank { null }
            )
        }
    }

    /**
     * 实时拉取 WBI 密钥并签名（对齐 BiliPai 原版）。
     */
    private suspend fun signWithWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
            if (keys != null) {
                WbiUtils.sign(params, keys.first, keys.second, includeRiskFingerprint = true)
            } else {
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

    private fun articleErrorMessage(code: Int, message: String): String {
        return when (code) {
            -412 -> "请求被 B 站拦截，稍后重试或用浏览器打开"
            -352 -> "请求频率过高（B站风控），请稍后重试"
            else -> message.ifBlank { "加载失败 ($code)" }
        }
    }
}

data class ArticleDetail(
    val cvId: Long,
    val title: String,
    val authorName: String,
    val publishTime: Long,
    val stats: Stats,
    val blocks: List<ArticleBlock>,
    val coverUrl: String? = null
)
