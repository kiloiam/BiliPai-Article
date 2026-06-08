package com.minipai.article

import android.app.Application
import android.util.Log
import com.minipai.article.core.database.AppDatabase
import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.WbiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用入口。
 * - 初始化 OkHttp/Retrofit 单例
 * - 异步预热 WBI 密钥（搜索接口必需）
 * - 注册全局未捕获异常 handler：写 logcat + 写文件（不依赖 adb）
 */
class ArticleApp : Application() {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // 0) 全局兜底：把崩溃堆栈同时打 logcat 和写文件，方便无 adb 时取。
        //    文件路径：/sdcard/Android/data/com.minipai.article/files/crash.log
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stack = Log.getStackTraceString(throwable)
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            runCatching {
                val crashDir = getExternalFilesDir(null) ?: filesDir
                val crashFile = File(crashDir, "crash.log")
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                crashFile.appendText(
                    """
                    ====================
                    Time:  $ts
                    Thread: ${thread.name}
                    $stack
                    """.trimIndent() + "\n"
                )
            }
            // 调用原 handler 让系统正常弹"App 已停止"对话框
            previousHandler?.uncaughtException(thread, throwable)
        }

        // 1) 注入 Context 到 NetworkModule（让 OkHttp cacheDir 可用）
        NetworkModule.init(this)

        // 2) 后台预热 WBI 密钥（24h TTL 内不会重复请求）
        applicationScope.launch {
            runCatching { WbiKeyManager.refreshIfNeeded() }
        }
    }

    companion object {
        private const val TAG = "BiliCrash"
    }
}
