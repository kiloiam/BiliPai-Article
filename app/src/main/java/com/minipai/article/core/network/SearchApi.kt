package com.minipai.article.core.network

import com.minipai.article.core.network.model.SearchArticleResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * 搜索相关 API。
 * Origin / Referer 由 OkHttp 拦截器统一注入（对齐 BiliPai 原版动态 Referer 模式）。
 */
interface SearchApi {
    /**
     * B 站专栏搜索（需 WBI 签名）。
     * URL: x/web-interface/wbi/search/type?search_type=article&keyword=...&page=...&...
     */
    @GET("x/web-interface/wbi/search/type")
    suspend fun searchArticle(@QueryMap params: Map<String, String>): SearchArticleResponse
}
