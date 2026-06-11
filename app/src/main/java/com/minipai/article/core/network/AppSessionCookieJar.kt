package com.minipai.article.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.UUID

/**
 * B 站 CookieJar，对齐 BiliPai 原版行为：
 * - 保存并回放所有服务器返回的 Cookie（buvid4、b_nut、_uuid 等）
 * - 自动生成 buvid3（UUID + "infoc" 格式）
 * - 不持久化 SESSDATA（无登录态）
 */
class AppSessionCookieJar : CookieJar {

    private val cookieLock = Any()
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val buvid3: String = UUID.randomUUID().toString() + "infoc"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(cookieLock) {
            val existing = cookieStore.getOrPut(url.host) { mutableListOf() }
            for (c in cookies) {
                existing.removeAll { it.name == c.name }
                existing.add(c)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        synchronized(cookieLock) {
            cookieStore[url.host]?.let { cookies.addAll(it) }
        }
        // 确保每次请求都带 buvid3（不存在时注入）
        if (cookies.none { it.name == "buvid3" }) {
            cookies.add(
                Cookie.Builder()
                    .domain(url.host)
                    .name("buvid3")
                    .value(buvid3)
                    .build()
            )
        }
        return cookies
    }
}
