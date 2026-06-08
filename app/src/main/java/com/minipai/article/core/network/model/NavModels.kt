package com.minipai.article.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NavResponse(
    val code: Int = 0,
    val message: String = "",
    val data: NavData? = null
)

@Serializable
data class NavData(
    val wbi_img: WbiImg? = null
)

@Serializable
data class WbiImg(
    val img_url: String = "",
    val sub_url: String = ""
)
