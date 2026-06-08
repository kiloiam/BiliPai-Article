package com.minipai.article.core.network

/**
 * 清理 B 站搜索结果文本中的 HTML 标签和实体：
 * - 移除 <em class="...">keyword</em> 高亮标签
 * - 解码常见 HTML 实体
 */
internal fun cleanSearchText(raw: String): String {
    return raw.replace(Regex("<.*?>"), "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

/**
 * 规范化搜索结果图片 URL：
 * - `//xxx.bilibili.com/...` → `https://xxx.bilibili.com/...`
 * - `http://...` → `https://...`
 * - 其他原样返回
 */
internal fun normalizeSearchImageUrl(raw: String): String {
    val text = raw.trim()
    return when {
        text.startsWith("//") -> "https:$text"
        text.startsWith("http://") -> text.replace("http://", "https://")
        else -> text
    }
}
