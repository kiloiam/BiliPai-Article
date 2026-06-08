package com.minipai.article.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * B 站 API 数字字段的"灵活"序列化器。
 * 数字有时返回 String、有时返回 Number、还会带"万"/"亿"中文单位——
 * 这里统一容忍这些情况，避免反序列化失败。
 */
private fun parseFlexibleNumber(raw: String): Double? {
    val text = raw.trim().replace(",", "")
    if (text.isEmpty()) return null
    return when {
        text.endsWith("万") -> text.removeSuffix("万").toDoubleOrNull()?.times(10_000.0)
        text.endsWith("亿") -> text.removeSuffix("亿").toDoubleOrNull()?.times(100_000_000.0)
        else -> text.toDoubleOrNull()
    }
}

object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder !is JsonDecoder) return decoder.decodeLong()
        val element = decoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return 0L
        val content = runCatching { primitive.content }.getOrNull() ?: return 0L
        content.trim().replace(",", "").toLongOrNull()?.let { return it }
        return parseFlexibleNumber(content)?.toLong() ?: 0L
    }
}

object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        if (decoder !is JsonDecoder) return decoder.decodeInt()
        val element = decoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return 0
        val content = runCatching { primitive.content }.getOrNull() ?: return 0
        return parseFlexibleNumber(content)?.toInt() ?: 0
    }
}
