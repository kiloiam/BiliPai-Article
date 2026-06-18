package com.minipai.article.core.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.minipai.article.core.network.model.NavResponse
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * x/web-interface/nav API（用于拉取 WBI 签名密钥）。
 */
interface NavApi {
    @GET("x/web-interface/nav")
    suspend fun getNavInfo(): NavResponse
}

/**
 * 网络层单例。
 * - OkHttp 注入 Chrome UA + buvid3 cookie（对齐 BiliPai 原版）
 * - 32MB 磁盘缓存
 */
object NetworkModule {

    private const val TAG = "NetworkModule"
    private const val CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val SEARCH_REFERER = "https://search.bilibili.com/"

    internal var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        WbiKeyManager.init(context.applicationContext)
        cookieJar.init(context.applicationContext)
    }

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val cookieJar: AppSessionCookieJar by lazy { AppSessionCookieJar() }

    /** 暴露给 WbiKeyManager 等内部组件复用（避免裸连 B 站触发风控） */
    val okHttpClient: OkHttpClient by lazy {
        val ctx = requireContext()
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(Cache(ctx.cacheDir.resolve("okhttp_cache"), 32L * 1024 * 1024))
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url
                val path = url.encodedPath

                // Origin 统一使用 www.bilibili.com（对齐 BiliPai 原版：拦截器中 Origin 始终为主站域名）
                // Referer 仅在非 WBI 路径设置；WBI 路径的 Referer 由 API 接口上的 @Headers 提供
                val isWbiEndpoint = path.contains("/wbi/")
                val referer = if (path.contains("/x/web-interface/")) SEARCH_REFERER else "https://www.bilibili.com"

                val builder = original.newBuilder()
                    .header("User-Agent", CHROME_UA)
                    .header("Origin", "https://www.bilibili.com")
                if (!isWbiEndpoint) {
                    builder.header("Referer", referer)
                }

                Log.d(TAG, "→ ${original.method} ${url.host}$path" +
                    ", Referer=${if (isWbiEndpoint) "OMITTED(WBI)" else referer}")
                chain.proceed(builder.build())
            }
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.bilibili.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val searchApi: SearchApi by lazy { retrofit.create(SearchApi::class.java) }
    val articleApi: ArticleApi by lazy { retrofit.create(ArticleApi::class.java) }
    val navApi: NavApi by lazy { retrofit.create(NavApi::class.java) }

    private var warmedUp = false

    /** 会话预热完成信号。SearchRepository 在发起请求前会等待此信号。 */
    val warmupReady = CompletableDeferred<Boolean>()

    /**
     * 会话预热：先访问 B 站首页建立 Cookie 档案，再拉 nav 建立 API 会话。
     * 冷启动直接搜专栏会被风控判定为爬虫。预热后 CookieJar 中带有完整的
     * buvid3 + buvid4 + b_nut + _uuid 等字段，WBI 签名才有效。
     */
    suspend fun warmup() = withContext(Dispatchers.IO) {
        if (warmedUp) return@withContext
        try {
            // 1) 访问 www.bilibili.com 获取浏览器级 Cookie
            val homeRequest = Request.Builder()
                .url("https://www.bilibili.com/")
                .header("User-Agent", CHROME_UA)
                .build()
            okHttpClient.newCall(homeRequest).execute().use { resp ->
                Log.d(TAG, "Warmup homepage: ${resp.code}, cookies=${resp.headers("Set-Cookie").size}")
            }
            // 2) 访问 nav 接口建立 API 会话 + 缓存 WBI 密钥
            val navResp = navApi.getNavInfo()
            // 同步预热 WbiKeyManager（避免首次搜索/打开专栏时额外请求 nav 触发 352）
            try {
                WbiKeyManager.refreshIfNeeded()
            } catch (_: Exception) { }
            warmedUp = true
            warmupReady.complete(true)
            Log.d(TAG, "Warmup complete")
        } catch (e: Exception) {
            Log.w(TAG, "Warmup failed: ${e.message}")
            warmupReady.complete(false)  // 预热失败不阻塞搜索
        }
    }

    private fun requireContext(): Context =
        appContext ?: error("NetworkModule.init(context) must be called first")
}
