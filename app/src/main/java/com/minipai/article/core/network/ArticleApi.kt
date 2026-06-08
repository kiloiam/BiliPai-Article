package com.minipai.article.core.network

import com.minipai.article.core.network.model.ArticleViewResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 专栏文章详情 API (`x/article/view`)。
 *
 * 该接口不在 `/wbi/` 路径下，不需要 WBI 签名。
 * 现有 `AppSessionCookieJar` 已自动注入 buvid3 cookie。
 */
interface ArticleApi {
    @GET("x/article/view")
    suspend fun getArticleView(@Query("id") id: Long): ArticleViewResponse
}
