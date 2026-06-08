package com.minipai.article

import android.app.Application
import com.minipai.article.core.database.AppDatabase
import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.WbiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口。
 * - 初始化 OkHttp/Retrofit 单例
 * - 异步预热 WBI 密钥（搜索接口必需）
 */
class ArticleApp : Application() {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // 1) 注入 Context 到 NetworkModule（让 OkHttp cacheDir 可用）
        NetworkModule.init(this)

        // 2) 后台预热 WBI 密钥（24h TTL 内不会重复请求）
        applicationScope.launch {
            runCatching { WbiKeyManager.refreshIfNeeded() }
        }
    }
}
