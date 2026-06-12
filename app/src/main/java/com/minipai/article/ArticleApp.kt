package com.minipai.article

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.minipai.article.core.database.AppDatabase
import com.minipai.article.core.network.NetworkModule
import com.minipai.article.core.network.WbiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用入口。
 * - 初始化 OkHttp/Retrofit 单例
 * - 注册全局未捕获异常 handler：写 logcat + 写文件（不依赖 adb）
 */
class ArticleApp : Application(), ImageLoaderFactory {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    /** 应用级协程 scope，用于后台预热等一次性任务；进程结束时 cancel */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // 1) 注入 Context 到 NetworkModule / WbiKeyManager
        NetworkModule.init(this)

        // 2) 从本地恢复 WBI 密钥（对齐 BiliPai 原版：同步 restore + 异步预刷）
        WbiKeyManager.restoreFromStorage(this)

        // 3) 后台预热 B 站会话 + 预刷 WBI 密钥
        appScope.launch {
            runCatching { NetworkModule.warmup() }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(512 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(4L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()
    }

    companion object {
        private const val TAG = "BiliCrash"
    }
}
