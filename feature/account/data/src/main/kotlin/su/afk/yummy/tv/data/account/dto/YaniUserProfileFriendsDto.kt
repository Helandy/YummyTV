package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserProfileFriendsDto(
    val friends: Int = 0,
    val requests: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val sentRequests: Int = 0,
)
