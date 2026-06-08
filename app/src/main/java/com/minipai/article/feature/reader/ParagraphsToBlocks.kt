package com.minipai.article.feature.reader

import com.minipai.article.core.network.model.Paragraph
import com.minipai.article.feature.reader.model.ArticleBlock
import com.minipai.article.feature.reader.model.TextSpan

/**
 * B 站 `opus.content.paragraphs` AST 直转 ArticleBlock。
 *
 * 优势：免 HTML 解析、字段语义清晰、不会丢样式。
 * 风险：字段名/形状若猜错，需按真实响应回填。
 */
object ParagraphsToBlocks {

    fun convert(paragraphs: List<Paragraph>): List<ArticleBlock> {
        if (paragraphs.isEmpty()) return emptyList()
        val out = ArrayList<ArticleBlock>(paragraphs.size)
        for (p in paragraphs) {
            when (p.paraType) {
                1 -> p.text?.let { text ->
                    val spans = buildSpans(text)
                    if (spans.isNotEmpty() && spans.any { it.text().isNotBlank() }) {
                        out.add(ArticleBlock.Paragraph(spans))
                    }
                }
                2 -> p.pic?.pics?.forEach { item ->
                    if (item.url.isNotBlank()) {
                        out.add(
                            ArticleBlock.Image(
                                url = item.url,
                                widthPx = item.width.takeIf { it > 0 },
                                heightPx = item.height.takeIf { it > 0 }
                            )
                        )
                    }
                }
                3 -> out.add(ArticleBlock.Divider)
                // 4=video, 5=vote, ... 暂不支持,跳过
                else -> { /* ignore */ }
            }
        }
        return out
    }

    private fun buildSpans(text: com.minipai.article.core.network.model.ParaText): List<TextSpan> {
        val spans = ArrayList<TextSpan>(text.nodes.size)
        for (node in text.nodes) {
            when (node.nodeType) {
                2 -> {
                    // link
                    val link = node.link
                    val url = link?.url.orEmpty()
                    val label = link?.text.takeIf { !it.isNullOrBlank() }
                        ?: node.word?.words.orEmpty()
                    if (label.isNotBlank() && url.isNotBlank()) {
                        spans.add(TextSpan.Link(label, url))
                    } else if (label.isNotBlank()) {
                        spans.add(TextSpan.Plain(label))
                    }
                }
                else -> {
                    // plain (and styled)
                    val w = node.word ?: continue
                    val str = w.words
                    if (str.isEmpty()) continue
                    val s = w.style
                    spans.add(
                        when {
                            s?.bold == true && s.italic == true -> TextSpan.Plain(str) // Bold+Italic 不复合,留 Plain 即可
                            s?.bold == true -> TextSpan.Bold(str)
                            s?.italic == true -> TextSpan.Italic(str)
                            else -> TextSpan.Plain(str)
                        }
                    )
                }
            }
        }
        return spans
    }

    private fun TextSpan.text(): String = when (this) {
        is TextSpan.Plain -> text
        is TextSpan.Bold -> text
        is TextSpan.Italic -> text
        is TextSpan.Link -> text
    }
}
