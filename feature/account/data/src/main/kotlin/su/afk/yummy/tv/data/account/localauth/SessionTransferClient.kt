package su.afk.yummy.tv.data.account.localauth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.di.LocalAuthHttpClient
import su.afk.yummy.tv.data.account.dto.SessionTransferDto
import su.afk.yummy.tv.data.account.dto.SessionTransferErrorDto
import su.afk.yummy.tv.data.account.utils.LocalAuthCrypto
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.SessionTransferException
import javax.inject.Inject

/** Отправка зашифрованной сессии на выбранный ТВ. */
internal class SessionTransferClient @Inject constructor(
    @LocalAuthHttpClient private val httpClient: HttpClient,
) {
    suspend fun transfer(device: DiscoveredDevice, pin: String, refreshToken: String) {
        withContext(Dispatchers.IO) {
            val payload = LocalAuthCrypto.encrypt(refreshToken, pin)
            val url = "http://${device.host}:${device.port}${LocalAuthContract.TRANSFER_PATH}"
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(SessionTransferDto(payload.data, payload.iv, payload.salt))
            }

            if (!response.status.isSuccess()) {
                throw SessionTransferException(
                    message = "Session transfer failed: ${response.status}",
                    reason = response.failureReason(),
                )
            }
        }
    }

    /** ТВ присылает имя [LocalAuthError] — телефон показывает причину тому, кто вводит PIN. */
    private suspend fun io.ktor.client.statement.HttpResponse.failureReason(): LocalAuthError? =
        runCatching { LocalAuthError.valueOf(body<SessionTransferErrorDto>().error) }.getOrNull()
}
