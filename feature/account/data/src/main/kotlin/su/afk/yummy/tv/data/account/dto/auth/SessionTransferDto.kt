package su.afk.yummy.tv.data.account.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionTransferDto(
    val encryptedToken: String,
    val iv: String,
    val salt: String,
)
