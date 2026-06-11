package com.minipai.article.core.network

import com.minipai.article.core.network.model.ArticleViewResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * 专栏文章详情 API (`x/article/view`)。
 * 对齐 BiliPai 原版：使用 @QueryMap 支持 WBI 签名参数。
 */
interface ArticleApi {
    @GET("x/article/view")
    suspend fun getArticleView(@QueryMap params: Map<String, String>): ArticleViewResponse
}
