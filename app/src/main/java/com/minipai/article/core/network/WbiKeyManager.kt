package com.minipai.article.core.network

import android.content.Context
import android.util.Log
import com.minipai.article.core.network.model.NavResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import kotlinx.serialization.json.Json

private const val TAG = "WbiKeyManager"
private const val SP_NAME = "wbi_keys_sp"
private const val SP_KEY_IMG = "wbi_img_key"
private const val SP_KEY_SUB = "wbi_sub_key"
private const val SP_KEY_TIMESTAMP = "wbi_timestamp"
private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
private const val PREFRESH_THRESHOLD_MS = 60 * 60 * 1000L

/** 用于拉取 wbi_img 的精简 Retrofit 客户端（无需复用 NetworkModule，避免循环依赖） */
private interface NavApi {
    @GET("https://api.bilibili.com/x/web-interface/nav")
    suspend fun getNavInfo(): NavResponse
}

private val navJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val navRetrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl("https://api.bilibili.com/")
        .addConverterFactory(navJson.asConverterFactory(okhttp3.MediaType.get("application/json")))
        .build()
}

private val navApi: NavApi by lazy { navRetrofit.create(NavApi::class.java) }

/**
 * WBI 签名密钥管理器。
 *
 * 24h 内存缓存 + SharedPreferences 持久化，避免每次搜索都请求 /x/web-interface/nav。
 */
object WbiKeyManager {

    @Volatile
    private var cachedKeys: Pair<String, String>? = null

    @Volatile
    private var cacheTimestamp: Long = 0

    private val refreshMutex = Mutex()

    @Volatile
    private var appContextRef: Context? = null

    fun init(context: Context) {
        appContextRef = context.applicationContext
        // 启动时尝试从 SharedPreferences 恢复
        restoreFromStorage(context.applicationContext)
    }

    suspend fun getWbiKeys(): Result<Pair<String, String>> {
        val cached = cachedKeys
        if (cached != null && isCacheValid()) {
            return Result.success(cached)
        }
        return refreshMutex.withLock {
            val rechecked = cachedKeys
            if (rechecked != null && isCacheValid()) {
                return@withLock Result.success(rechecked)
            }
            refreshKeysInternal()
        }
    }

    suspend fun refreshIfNeeded() {
        getWbiKeys()
    }

    private suspend fun refreshKeysInternal(): Result<Pair<String, String>> {
        return try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img

            if (wbiImg != null) {
                val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
                val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")

                cachedKeys = Pair(imgKey, subKey)
                cacheTimestamp = System.currentTimeMillis()

                appContextRef?.let { ctx ->
                    runCatching { persistToStorage(ctx) }
                }

                Log.d(TAG, "WBI keys refreshed: imgKey=$imgKey")
                Result.success(Pair(imgKey, subKey))
            } else {
                Log.e(TAG, "WBI keys not found in nav response")
                Result.failure(Exception("WBI keys not found in nav response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh WBI keys: ${e.message}")
            Result.failure(e)
        }
    }

    fun invalidateCache() {
        cachedKeys = null
        cacheTimestamp = 0
    }

    private fun isCacheValid(): Boolean {
        val age = System.currentTimeMillis() - cacheTimestamp
        return age < CACHE_DURATION_MS
    }

    fun shouldPrefresh(): Boolean {
        val remaining = (cacheTimestamp + CACHE_DURATION_MS) - System.currentTimeMillis()
        return remaining < PREFRESH_THRESHOLD_MS
    }

    private fun persistToStorage(context: Context) {
        val keys = cachedKeys ?: return
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit()
            .putString(SP_KEY_IMG, keys.first)
            .putString(SP_KEY_SUB, keys.second)
            .putLong(SP_KEY_TIMESTAMP, cacheTimestamp)
            .apply()
    }

    private fun restoreFromStorage(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val imgKey = sp.getString(SP_KEY_IMG, null)
        val subKey = sp.getString(SP_KEY_SUB, null)
        val timestamp = sp.getLong(SP_KEY_TIMESTAMP, 0)

        if (imgKey != null && subKey != null && timestamp > 0) {
            cachedKeys = Pair(imgKey, subKey)
            cacheTimestamp = timestamp

            if (isCacheValid()) {
                Log.d(TAG, "WBI keys restored from storage")
                return true
            } else {
                invalidateCache()
            }
        }
        return false
    }
}
