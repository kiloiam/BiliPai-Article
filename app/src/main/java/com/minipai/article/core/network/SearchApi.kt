package com.minipai.article.core.network

import com.minipai.article.core.network.model.SearchArticleResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.QueryMap

/**
 * 搜索相关 API。
 *
 * 对齐 BiliPai 原版：@Headers 提供 Referer（拦截器会跳过 WBI 路径的 Referer 设置，
 * 但 @Headers 中的 Referer 不会被覆盖），Origin 由拦截器统一设为 www.bilibili.com。
 */
interface SearchApi {
    @Headers(
        "Origin: https://search.bilibili.com",
        "Referer: https://search.bilibili.com/"
    )
    @GET("x/web-interface/wbi/search/type")
    suspend fun searchArticle(@QueryMap params: Map<String, String>): SearchArticleResponse
}
