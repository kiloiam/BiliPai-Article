package com.minipai.article.core.network.model

import com.minipai.article.core.network.FlexibleIntSerializer
import com.minipai.article.core.network.FlexibleLongSerializer
import com.minipai.article.core.network.cleanSearchText
import com.minipai.article.core.network.normalizeSearchImageUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchArticleResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SearchArticleData? = null
)

@Serializable
data class SearchArticleData(
    val page: Int = 1,
    val pagesize: Int = 20,
    val numResults: Int = 0,
    val numPages: Int = 0,
    val result: List<SearchArticleItem>? = null
)

@Serializable
data class SearchArticleItem(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0,
    @Serializable(with = FlexibleLongSerializer::class)
    val mid: Long = 0,
    val title: String = "",
    @SerialName("desc")
    val description: String = "",
    @SerialName("pub_time")
    @Serializable(with = FlexibleLongSerializer::class)
    val pubTime: Long = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val view: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val reply: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val like: Int = 0,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("category_name")
    val categoryName: String = "",
    @SerialName("category_id")
    @Serializable(with = FlexibleIntSerializer::class)
    val categoryId: Int = 0
) {
    fun cleanupFields(): SearchArticleItem {
        return copy(
            title = cleanSearchText(title),
            imageUrls = imageUrls.mapNotNull { url ->
                val normalized = normalizeSearchImageUrl(url)
                if (normalized.isBlank()) null else normalized
            }
        )
    }
}
