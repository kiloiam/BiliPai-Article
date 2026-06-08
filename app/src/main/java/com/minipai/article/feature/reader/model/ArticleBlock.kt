package com.minipai.article.feature.reader.model

/**
 * 阅读器块模型。LazyColumn 直接 `items(blocks)` 渲染。
 * - `Paragraph` / `Heading` / `Quote` / `ListBlock` 都由 `List<TextSpan>` 构成，
 *   inline 样式（粗体/斜体/链接）由 TextSpan 表达。
 * - `Image` / `Divider` / `Code` 是 leaf 节点。
 */
sealed class ArticleBlock {
    data class Paragraph(val spans: List<TextSpan>) : ArticleBlock()
    data class Heading(val level: Int, val text: String) : ArticleBlock()
    data class Image(val url: String, val widthPx: Int? = null, val heightPx: Int? = null) : ArticleBlock()
    data class Quote(val spans: List<TextSpan>) : ArticleBlock()
    data class Code(val lang: String? = null, val code: String) : ArticleBlock()
    data class ListBlock(val ordered: Boolean, val items: List<List<TextSpan>>) : ArticleBlock()
    object Divider : ArticleBlock()
}

sealed class TextSpan {
    data class Plain(val text: String) : TextSpan()
    data class Bold(val text: String) : TextSpan()
    data class Italic(val text: String) : TextSpan()
    data class Link(val text: String, val url: String) : TextSpan()
}
