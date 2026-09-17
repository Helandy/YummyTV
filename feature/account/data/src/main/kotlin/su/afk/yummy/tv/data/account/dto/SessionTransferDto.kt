package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionTransferDto(
    val encryptedToken: String,
    val iv: String,
    val salt: String,
)

/** Успешный ответ локального сервера сопряжения. */
@Serializable
data class SessionTransferOkDto(val status: String = "ok")

/** Причина отказа: имя [su.afk.yummy.tv.domain.account.model.LocalAuthError], не текст для показа. */
@Serializable
data class SessionTransferErrorDto(val error: String)
