package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniProfilePrivacyDto(
    @SerialName("shiki_public") val shikimoriPublic: Boolean = true,
    @SerialName("tg_public") val telegramPublic: Boolean = true,
    @SerialName("vk_public") val vkPublic: Boolean = true,
    @SerialName("discord_public") val discordPublic: Boolean = true,
)
