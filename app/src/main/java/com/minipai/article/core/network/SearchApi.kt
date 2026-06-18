package com.minipai.article.core.network

import com.minipai.article.core.network.model.SearchArticleResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.QueryMap

/**
 * 搜索相关 API。
 *
 * Origin 由 OkHttp 拦截器统一设置，Referer 由 @Headers 提供（拦截器跳过 WBI 路径的 Referer，
 * 对齐 BiliPai 原版 AppSessionCookieJar + Interceptors 行为）。
 */
interface SearchApi {
    @Headers("Referer: https://search.bilibili.com/")
    @GET("x/web-interface/wbi/search/type")
    suspend fun searchArticle(@QueryMap params: Map<String, String>): SearchArticleResponse
}
