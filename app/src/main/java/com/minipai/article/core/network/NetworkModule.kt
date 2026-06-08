package com.minipai.article.core.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层单例。
 * - OkHttp 注入 Chrome UA + buvid3 cookie（必须，否则返回 412）
 * - 32MB 磁盘缓存
 * - WBI 端点跳过 Referer 头（B 站强制）
 */
object NetworkModule {

    private const val TAG = "NetworkModule"
    private const val CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val SEARCH_ORIGIN = "https://search.bilibili.com"
    private const val SEARCH_REFERER = "https://search.bilibili.com/"

    internal var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        WbiKeyManager.init(context.applicationContext)
    }

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val cookieJar: AppSessionCookieJar by lazy {
        AppSessionCookieJar(requireContext())
    }

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
                val isSearchEndpoint = url.encodedPath.contains("/search/")

                val builder = original.newBuilder()
                    .header("User-Agent", CHROME_UA)

                if (isSearchEndpoint) {
                    builder.header("Origin", SEARCH_ORIGIN)
                    // WBI 端点（/x/web-interface/wbi/...）不能带 Referer
                    if (!url.encodedPath.contains("/wbi/")) {
                        builder.header("Referer", SEARCH_REFERER)
                    }
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.bilibili.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(okhttp3.MediaType.get("application/json")))
            .build()
    }

    val searchApi: SearchApi by lazy { retrofit.create(SearchApi::class.java) }
    val articleApi: ArticleApi by lazy { retrofit.create(ArticleApi::class.java) }

    private fun requireContext(): Context =
        appContext ?: error("NetworkModule.init(context) must be called first")
}
