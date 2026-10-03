package su.afk.yummy.tv.data.account.dto.auth

import kotlinx.serialization.Serializable

/** Успешный ответ локального сервера сопряжения. */
@Serializable
data class SessionTransferOkDto(val status: String = "ok")
