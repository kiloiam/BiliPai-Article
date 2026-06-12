package com.minipai.article.core.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.UUID

/**
 * B 站 CookieJar，对齐 BiliPai 原版行为：
 * - 保存并回放所有服务器返回的 Cookie（buvid4、b_nut、_uuid 等）
 * - 自动生成 buvid3（UUID + "infoc" 格式），通过 SharedPreferences 持久化
 * - 预留 SESSDATA / bili_jct 注入（未来登录支持）
 */
class AppSessionCookieJar : CookieJar {

    private val cookieLock = Any()
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    @Volatile
    private var persistedBuvid3: String? = null

    @Volatile
    private var sessData: String? = null

    @Volatile
    private var biliJct: String? = null

    /** 用已持久化的 buvid3 初始化。NetworkModule.init 中调用。 */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        persistedBuvid3 = prefs.getString(KEY_BUVID3, null)
        if (persistedBuvid3 == null) {
            persistedBuvid3 = UUID.randomUUID().toString() + "infoc"
            prefs.edit().putString(KEY_BUVID3, persistedBuvid3).apply()
        }
    }

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

        // 注入持久化的 buvid3（对齐 BiliPai TokenManager.buvid3Cache 模式）
        val buvid3 = persistedBuvid3 ?: run {
            val fallback = UUID.randomUUID().toString() + "infoc"
            persistedBuvid3 = fallback
            fallback
        }
        if (cookies.none { it.name == "buvid3" }) {
            cookies.add(
                Cookie.Builder()
                    .domain(url.host)
                    .name("buvid3")
                    .value(buvid3)
                    .build()
            )
        }

        // 注入 SESSDATA / bili_jct（如有登录态）
        val host = if (url.host.endsWith("bilibili.com")) "bilibili.com" else url.host
        sessData?.let { sd ->
            cookies.removeAll { it.name == "SESSDATA" }
            cookies.add(
                Cookie.Builder()
                    .domain(host)
                    .name("SESSDATA")
                    .value(sd)
                    .build()
            )
        }
        biliJct?.let { bj ->
            cookies.removeAll { it.name == "bili_jct" }
            cookies.add(
                Cookie.Builder()
                    .domain(host)
                    .name("bili_jct")
                    .value(bj)
                    .build()
            )
        }

        return cookies
    }

    /** 清除运行时 Cookie（对齐 NetworkModule.clearRuntimeCookies） */
    fun clear() {
        synchronized(cookieLock) {
            cookieStore.clear()
        }
    }

    companion object {
        private const val PREFS_NAME = "buvid3_prefs"
        private const val KEY_BUVID3 = "buvid3"
    }
}
