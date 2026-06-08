package com.minipai.article.core.network.model

import com.minipai.article.core.network.FlexibleIntSerializer
import com.minipai.article.core.network.FlexibleLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * B 站专栏详情 `x/article/view` 响应模型。
 *
 * 正文有 3 种格式冗余,按优先级使用:
 * 1. `data.opus.content.paragraphs` — 结构化 AST (最新,免解析)
 * 2. `data.opus.h5_content` — HTML 字符串 (新)
 * 3. `data.content` — HTML 字符串 (旧)
 *
 * 所有字段都给了默认值,配合 `ignoreUnknownKeys=true` 容错。
 */
@Serializable
data class ArticleViewResponse(
    val code: Int = 0,
    val message: String = "",
    val data: ArticleViewData? = null
)

@Serializable
data class ArticleViewData(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0,
    val title: String = "",
    @SerialName("publish_time")
    @Serializable(with = FlexibleLongSerializer::class)
    val publishTime: Long = 0,
    @SerialName("banner_url")
    val bannerUrl: String = "",
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    val author: Author = Author(),
    val stats: Stats = Stats(),
    /** 旧版 HTML 正文 (兜底) */
    val content: String = "",
    /** 新版 opus 容器,优先使用 */
    val opus: Opus? = null
)

@Serializable
data class Author(
    @Serializable(with = FlexibleLongSerializer::class)
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class Stats(
    @Serializable(with = FlexibleIntSerializer::class)
    val view: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val like: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val reply: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val favorite: Int = 0
)

@Serializable
data class Opus(
    val content: OpusContent? = null,
    @SerialName("h5_content")
    val h5Content: String = ""
)

@Serializable
data class OpusContent(
    val paragraphs: List<Paragraph> = emptyList()
)

/**
 * 段落级 AST 节点,`para_type` 决定类型:
 * - 1 = 文本 (有 `text.nodes[]`)
 * - 2 = 图片 (有 `pic.pics[]`)
 * - 3 = 分隔线 (有 `line`)
 * - 其他 (视频/投票) 暂不支持
 */
@Serializable
data class Paragraph(
    @SerialName("para_type")
    val paraType: Int = 0,
    val text: ParaText? = null,
    val pic: ParaPic? = null,
    val line: ParaLine? = null
)

@Serializable
data class ParaText(
    val nodes: List<ParaNode> = emptyList()
)

@Serializable
data class ParaNode(
    @SerialName("node_type")
    val nodeType: Int = 0,
    val word: ParaWord? = null,
    /** link 类型节点专用 */
    val link: ParaLink? = null
)

@Serializable
data class ParaWord(
    val words: String = "",
    val style: ParaStyle? = null
)

@Serializable
data class ParaStyle(
    val bold: Boolean = false,
    val italic: Boolean = false
)

@Serializable
data class ParaLink(
    val text: String = "",
    val url: String = ""
)

@Serializable
data class ParaPic(
    val pics: List<ParaPicItem> = emptyList()
)

@Serializable
data class ParaPicItem(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class ParaLine(
    val placeholder: String = ""
)
