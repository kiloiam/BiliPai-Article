package com.minipai.article.core.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import com.minipai.article.core.network.model.NavResponse
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
                val builder = original.newBuilder()
                    .header("User-Agent", CHROME_UA)
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

    private fun requireContext(): Context =
        appContext ?: error("NetworkModule.init(context) must be called first")
}
