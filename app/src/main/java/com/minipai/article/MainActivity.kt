package com.minipai.article

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minipai.article.core.ui.theme.BiliPaiArticleTheme
import com.minipai.article.feature.reader.ArticleScreen
import com.minipai.article.feature.search.SearchScreen

/**
 * 单 Activity 入口。
 * - NavHost 串起搜索页和阅读页
 * - SharedTransitionLayout 提供 hero 动画（结果卡片 → 阅读页）
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiliPaiArticleTheme {
                BiliPaiApp()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BiliPaiApp() {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = "search"
            ) {
                composable("search") {
                    SearchScreen(
                        onOpenArticle = { cvId ->
                            navController.navigate("article/$cvId")
                        }
                    )
                }
                composable("article/{cvId}") { backStackEntry ->
                    val cvId = backStackEntry.arguments?.getString("cvId")?.toLongOrNull() ?: 0L
                    ArticleScreen(
                        cvId = cvId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
