package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserFriendDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniAvatarDto? = null,
    @SerialName("last_online") val lastOnline: Long = 0L,
    @SerialName("friend_status") val friendStatus: String = "",
    val list: String = "",
)
