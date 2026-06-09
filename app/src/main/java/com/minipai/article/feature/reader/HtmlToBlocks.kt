package com.minipai.article.feature.reader

import com.minipai.article.core.network.normalizeSearchImageUrl
import com.minipai.article.feature.reader.model.ArticleBlock
import com.minipai.article.feature.reader.model.TextSpan

/**
 * 极简 HTML → ArticleBlock 转换（无依赖，覆盖 B 站专栏 90% 场景）。
 *
 * 设计取舍：
 * - 用 regex 切 token；维护一个 `Deque<String>` 跟踪当前父标签栈
 * - 块级标签（p/h1-h4/pre/blockquote/ul/ol/li/hr/img）触发刷新成 Block
 * - inline 标签（strong/em/a/br）累积为 TextSpan
 * - 实体解码：`&nbsp; &amp; &lt; &gt; &quot; &#数字;`
 * - 未知标签 / 嵌套表格 / SVG / 行内 style：直接当文本流过（保守不丢内容）
 *
 * 已知限制：样式都按 plain 处理，不保留 inline 颜色/字号/链接点击（链接 url 会丢）。
 */
object HtmlToBlocks {

    private val tagRegex = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>""")
    private val voidTags = setOf("br", "hr", "img", "meta", "link", "input")

    fun convert(html: String): List<ArticleBlock> {
        if (html.isBlank()) return emptyList()
        val blocks = ArrayList<ArticleBlock>()
        // 状态
        val stack: ArrayDeque<String> = ArrayDeque()
        val spanStack: ArrayDeque<MutableList<TextSpan>> = ArrayDeque()
        var currentSpans: MutableList<TextSpan>? = null
        // list 临时缓冲
        var listOrdered = false
        var listBuffer: MutableList<List<TextSpan>>? = null

        fun flushParagraph() {
            val spans = currentSpans ?: return
            if (spans.isNotEmpty() && spans.any { textOf(it).isNotBlank() }) {
                blocks.add(ArticleBlock.Paragraph(spans.toList()))
            }
            currentSpans = null
        }

        fun flushList() {
            val buf = listBuffer ?: return
            if (buf.isNotEmpty()) {
                blocks.add(ArticleBlock.ListBlock(ordered = listOrdered, items = buf.toList()))
            }
            listBuffer = null
        }

        fun ensureCurrent(): MutableList<TextSpan> {
            if (currentSpans == null) currentSpans = ArrayList()
            return currentSpans!!
        }

        fun pushSpan(parent: String): MutableList<TextSpan> {
            val list = ArrayList<TextSpan>()
            spanStack.addLast(list)
            return list
        }

        // 先把 <br> 和 <hr> 之类的 self-closing 解析掉，正则匹配时单独识别
        val tokens = tagRegex.findAll(html)

        var cursor = 0
        for (m in tokens) {
            // 1) 标签之前的纯文本
            if (m.range.first > cursor) {
                val text = decodeEntities(html.substring(cursor, m.range.first))
                if (text.isNotEmpty()) {
                    val target = currentSpans ?: ensureCurrent()
                    target.add(TextSpan.Plain(text))
                }
            }
            cursor = m.range.last + 1

            val isClose = m.groupValues[1] == "/"
            val tag = m.groupValues[2].lowercase()
            val attrs = m.groupValues[3]

            // 处理 void tag
            if (!isClose && tag in voidTags) {
                when (tag) {
                    "br" -> {
                        val target = currentSpans ?: ensureCurrent()
                        target.add(TextSpan.Plain("\n"))
                    }
                    "hr" -> {
                        flushParagraph()
                        flushList()
                        blocks.add(ArticleBlock.Divider)
                    }
                    "img" -> {
                        val src = normalizeSearchImageUrl(extractAttr(attrs, "src").orEmpty())
                        if (src.isNotBlank()) {
                            flushParagraph()
                            flushList()
                            blocks.add(ArticleBlock.Image(src))
                        }
                    }
                }
                continue
            }

            if (isClose) {
                // 闭合标签
                when (tag) {
                    "p", "h1", "h2", "h3", "h4" -> {
                        flushParagraph()
                    }
                    "pre" -> {
                        flushParagraph() // 简单处理：当成普通段落
                    }
                    "blockquote" -> {
                        val spans = currentSpans
                        flushParagraph()
                        if (!spans.isNullOrEmpty() && spans.any { textOf(it).isNotBlank() }) {
                            blocks.add(ArticleBlock.Quote(spans.toList()))
                        }
                    }
                    "ul", "ol" -> {
                        flushList()
                    }
                    "li" -> {
                        val item = currentSpans
                        flushParagraph()
                        if (item != null && item.any { textOf(it).isNotBlank() }) {
                            listBuffer?.add(item.toList())
                        }
                    }
                    "strong", "b" -> {
                        // 把 spanStack 顶层的 spans 提到当前层,作为 Bold
                        if (spanStack.isNotEmpty()) {
                            val top = spanStack.removeLast()
                            val current = ensureCurrent()
                            top.forEach { sp ->
                                val t = textOf(sp)
                                if (t.isNotEmpty()) current.add(TextSpan.Bold(t))
                            }
                        }
                    }
                    "em", "i" -> {
                        if (spanStack.isNotEmpty()) {
                            val top = spanStack.removeLast()
                            val current = ensureCurrent()
                            top.forEach { sp ->
                                val t = textOf(sp)
                                if (t.isNotEmpty()) current.add(TextSpan.Italic(t))
                            }
                        }
                    }
                    "a" -> {
                        if (spanStack.isNotEmpty()) {
                            val top = spanStack.removeLast()
                            val current = ensureCurrent()
                            val joined = top.joinToString("") { textOf(it) }
                            if (joined.isNotBlank()) {
                                // url 已在开标签压栈时记下
                                val url = (top.firstOrNull() as? TextSpan.Link)?.url ?: ""
                                if (url.isNotBlank()) {
                                    current.add(TextSpan.Link(joined, url))
                                } else {
                                    current.add(TextSpan.Plain(joined))
                                }
                            }
                        }
                    }
                }
                // 弹栈
                if (stack.isNotEmpty() && stack.last() == tag) {
                    stack.removeLast()
                }
            } else {
                // 开标签
                when (tag) {
                    "p", "h1", "h2", "h3", "h4" -> {
                        flushList()
                        // 新的段落 currentSpans
                    }
                    "h1", "h2", "h3", "h4" -> { /* handled above */ }
                    "pre" -> {
                        flushList()
                        flushParagraph()
                        // 简化：把 pre 后的文字当 paragraph
                    }
                    "blockquote" -> {
                        flushList()
                        flushParagraph()
                    }
                    "ul" -> {
                        flushParagraph()
                        listOrdered = false
                        listBuffer = ArrayList()
                    }
                    "ol" -> {
                        flushParagraph()
                        listOrdered = true
                        listBuffer = ArrayList()
                    }
                    "li" -> {
                        flushParagraph()
                    }
                    "strong", "b" -> {
                        pushSpan(tag)
                    }
                    "em", "i" -> {
                        pushSpan(tag)
                    }
                    "a" -> {
                        val href = extractAttr(attrs, "href").orEmpty()
                        val list = pushSpan(tag)
                        if (href.isNotBlank()) {
                            // 借用 Link 存 url,会在闭合时被 join
                            list.add(TextSpan.Link("", href))
                        }
                    }
                    else -> { /* 未知标签忽略 */ }
                }
                stack.addLast(tag)
            }
        }

        // 收尾
        if (cursor < html.length) {
            val text = decodeEntities(html.substring(cursor))
            if (text.isNotEmpty()) {
                val target = currentSpans ?: ensureCurrent()
                target.add(TextSpan.Plain(text))
            }
        }
        flushParagraph()
        flushList()
        return blocks
    }

    private fun textOf(span: TextSpan): String = when (span) {
        is TextSpan.Plain -> span.text
        is TextSpan.Bold -> span.text
        is TextSpan.Italic -> span.text
        is TextSpan.Link -> span.text
    }

    private fun extractAttr(attrs: String, name: String): String? {
        // 容忍 'name=...', "name=...", name='...'
        val regex = Regex("""(?i)\b$name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]*))""")
        val m = regex.find(attrs) ?: return null
        return m.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
    }

    private fun decodeEntities(s: String): String {
        if (s.indexOf('&') < 0) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '&') {
                sb.append(c); i++; continue
            }
            val semi = s.indexOf(';', i + 1)
            if (semi < 0 || semi - i > 8) {
                sb.append(c); i++; continue
            }
            val ent = s.substring(i + 1, semi)
            val decoded: String? = when (ent) {
                "nbsp" -> " "
                "amp" -> "&"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "apos" -> "'"
                else -> {
                    if (ent.startsWith("#")) {
                        val num = ent.substring(1)
                        val cp = if (num.startsWith("x") || num.startsWith("X")) {
                            num.substring(1).toIntOrNull(16)
                        } else num.toIntOrNull()
                        cp?.let { Character.toString(it) }
                    } else null
                }
            }
            if (decoded != null) {
                sb.append(decoded)
                i = semi + 1
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}
