package com.minipai.article.data

import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.model.Stats
import com.minipai.article.feature.reader.HtmlToBlocks
import com.minipai.article.feature.reader.ParagraphsToBlocks
import com.minipai.article.feature.reader.model.ArticleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 拉取并解析专栏详情。
 *
 * 正文有 3 种格式冗余,按优先级使用:
 * 1. `opus.content.paragraphs` — 结构化 AST (免解析)
 * 2. `opus.h5_content` — HTML 兜底
 * 3. `content` — 老版 HTML 兜底
 */
class ArticleRepository {

    suspend fun loadArticle(cvId: Long): Result<ArticleDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = NetworkModule.articleApi.getArticleView(cvId)
            if (resp.code != 0) {
                error("加载失败 (${resp.code})${resp.message.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}")
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
                coverUrl = d.bannerUrl.takeIf { it.isNotBlank() }
                    ?: d.imageUrls.firstOrNull()
            )
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
