package com.minipai.article.core.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.UUID

/**
 * B 站 CookieJar：
 * - 不登录态（不持久化 SESSDATA）
 * - 自动生成并注入 buvid3（B 站强制反爬字段，缺失触发 412）
 * - 保留服务器返回的非登录 Cookie（b_nut、buvid4、_uuid 等辅助 WBI 风控）
 * - 进程内缓存，重启后重新生成
 */
class AppSessionCookieJar(context: Context) : CookieJar {

    private val buvid3: String = generateBuvid3()
    private val serverCookies = mutableListOf<Cookie>()

    private val buvid3Cookie: Cookie by lazy {
        Cookie.Builder()
            .name("buvid3")
            .value(buvid3)
            .domain("bilibili.com")
            .path("/")
            .build()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.host.endsWith("bilibili.com")) return emptyList()
        val cookies = mutableListOf(buvid3Cookie)
        // 附带服务器返回的非登录 cookie（如 b_nut、buvid4、_uuid）
        val expired = mutableListOf<Cookie>()
        for (c in serverCookies) {
            if (c.expiresAt > System.currentTimeMillis()) {
                if (c.matches(url)) cookies.add(c)
            } else {
                expired.add(c)
            }
        }
        serverCookies.removeAll(expired)
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) {
            // 跳过登录态 session，只保留辅助反爬 cookie
            if (c.name in setOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5")) continue
            serverCookies.removeAll { it.name == c.name && it.domain == c.domain }
            serverCookies.add(c)
        }
    }

    private fun generateBuvid3(): String {
        val uuid = UUID.randomUUID().toString().uppercase()
        val timePart = System.currentTimeMillis().toString(16).uppercase()
        val randPart = (1..16).map { "0123456789ABCDEF".random() }.joinToString("")
        val infoc = "${uuid}${timePart}${randPart}"
        return "${infoc}infoc"
    }
}
