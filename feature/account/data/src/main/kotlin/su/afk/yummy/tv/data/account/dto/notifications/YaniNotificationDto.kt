package su.afk.yummy.tv.data.account.dto.notifications

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniNotificationDto(
    val id: Int = 0,
    val date: Long = 0L,
    @SerialName("text_html") val textHtml: String = "",
    @SerialName("title_html") val titleHtml: String = "",
    @SerialName("click_uri") val clickUri: String = "",
    val type: String = "",
    @SerialName("sub_type") val subType: String = "",
    val viewed: Boolean = false,
    @SerialName("object_id") val objectId: Int? = null,
)
