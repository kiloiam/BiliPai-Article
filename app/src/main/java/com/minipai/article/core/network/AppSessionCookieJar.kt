package com.minipai.article.core.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.UUID

/**
 * 极简 CookieJar：
 * - 不登录态，不持久化 SESSDATA
 * - 仅生成并注入 buvid3（B 站强制反爬字段，缺失会触发 412）
 * - 进程内缓存，其他字段透传为空
 */
class AppSessionCookieJar(context: Context) : CookieJar {

    private val buvid3: String = generateBuvid3()
    private val buvid3Cookie: Cookie by lazy {
        Cookie.Builder()
            .name("buvid3")
            .value(buvid3)
            .domain(".bilibili.com")
            .path("/")
            .build()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 只在调用 *.bilibili.com 时注入 buvid3
        return if (url.host.endsWith("bilibili.com")) {
            listOf(buvid3Cookie)
        } else {
            emptyList()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // 不登录态：忽略服务器返回的 session cookie
    }

    private fun generateBuvid3(): String {
        // buvid3 = UUID + 时间戳 hex + 随机 16 进制，模拟浏览器生成
        val uuid = UUID.randomUUID().toString().uppercase()
        val timePart = System.currentTimeMillis().toString(16).uppercase()
        val randPart = (1..16).map { "0123456789ABCDEF".random() }.joinToString("")
        val infoc = "${uuid}${timePart}${randPart}"
        return "${infoc}infoc"
    }
}
