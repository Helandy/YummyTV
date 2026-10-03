package su.afk.yummy.tv.data.account.dto.auth

import kotlinx.serialization.Serializable

/** Причина отказа: имя [su.afk.yummy.tv.domain.account.model.LocalAuthError], не текст для показа. */
@Serializable
data class SessionTransferErrorDto(val error: String)
