package com.minipai.article.core.network

import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * 专栏文章详情 API（备用）。
 * 当前极简版阅读走 WebView，但保留此接口供未来扩展（自建 HTML 渲染等）。
 */
interface ArticleApi {
    @GET("x/article/view")
    suspend fun getArticleView(@QueryMap params: Map<String, String>): Unit
}
