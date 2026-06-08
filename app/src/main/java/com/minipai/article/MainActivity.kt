package com.minipai.article

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minipai.article.core.ui.theme.BiliPaiArticleTheme
import com.minipai.article.feature.me.MyScreen
import com.minipai.article.feature.reader.ArticleScreen
import com.minipai.article.feature.search.SearchScreen

/**
 * 单 Activity 入口。
 * - NavHost 串起搜索页 / 我的页 / 阅读页
 * - SharedTransitionLayout 提供 hero 动画（结果卡片 → 阅读页）
 * - 底部 NavigationBar：搜索 / 我的（阅读页沉浸，隐藏底栏）
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 只在「搜索」/「我的」显示底栏；阅读页沉浸
    val showBottomBar = currentRoute == "search" || currentRoute == "me"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == "search",
                            onClick = {
                                if (currentRoute != "search") {
                                    navController.navigate("search") {
                                        popUpTo("search") { inclusive = true }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            label = { Text("搜索") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "me",
                            onClick = {
                                if (currentRoute != "me") {
                                    navController.navigate("me") {
                                        popUpTo("search")
                                    }
                                }
                            },
                            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                            label = { Text("我的") }
                        )
                    }
                }
            }
        ) { padding ->
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = "search",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("search") {
                        SearchScreen(
                            onOpenArticle = { cvId ->
                                navController.navigate("article/$cvId")
                            }
                        )
                    }
                    composable("me") {
                        MyScreen(
                            onOpenArticle = { cvId ->
                                navController.navigate("article/$cvId")
                            }
                        )
                    }
                    composable("article/{cvId}") { entry ->
                        val cvId = entry.arguments?.getString("cvId")?.toLongOrNull() ?: 0L
                        ArticleScreen(
                            cvId = cvId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
