package com.minipai.article.core.network

import com.minipai.article.core.network.model.SearchArticleResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.QueryMap

/**
 * 搜索相关 API。
 * 当前极简版只暴露专栏搜索 (search_type=article)。
 * 新接口请按相同模式（@Headers + @GET + @QueryMap）追加。
 */
interface SearchApi {
    /**
     * B 站专栏搜索（需 WBI 签名）。
     * URL: x/web-interface/wbi/search/type?search_type=article&keyword=...&page=...&...
     */
    @GET("x/web-interface/wbi/search/type")
    suspend fun searchArticle(@QueryMap params: Map<String, String>): SearchArticleResponse
}
