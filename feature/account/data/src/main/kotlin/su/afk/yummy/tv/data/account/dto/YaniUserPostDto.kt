package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserPostDto(
    val id: Int = 0,
    val title: String = "",
    @SerialName("preview_image") val previewImage: String? = null,
    @SerialName("content_preview") val contentPreview: String = "",
    val category: YaniPostCategoryDto? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
)
