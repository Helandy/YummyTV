package su.afk.yummy.tv.data.account.localauth

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.account.dto.SessionTransferDto
import su.afk.yummy.tv.data.account.dto.SessionTransferErrorDto
import su.afk.yummy.tv.data.account.dto.SessionTransferOkDto
import su.afk.yummy.tv.data.account.utils.LocalAuthCrypto
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/**
 * Локальный HTTP-сервер ТВ, принимающий сессию с телефона.
 * Запуск блокирующий — вызывать на [kotlinx.coroutines.Dispatchers.IO].
 */
internal class LocalAuthServer @Inject constructor(
    private val json: Json,
    private val accountRepository: AccountRepository,
) {

    suspend fun start(session: PairingSession, onState: (LocalAuthServerState) -> Unit): Handle {
        val handle = Handle()
        val engine = embeddedServer(CIO, port = ANY_FREE_PORT) {
            install(ContentNegotiation) { json(json) }
            routing {
                post(LocalAuthContract.TRANSFER_PATH) {
                    handleTransfer(session, handle, onState)
                }
            }
        }.start(wait = false)

        handle.attach(engine)
        return handle
    }

    private suspend fun RoutingContext.handleTransfer(
        session: PairingSession,
        handle: Handle,
        onState: (LocalAuthServerState) -> Unit,
    ) {
        when (session.rejection()) {
            PairingSession.Rejection.EXPIRED -> {
                onState(LocalAuthServerState.Error(LocalAuthError.PIN_EXPIRED))
                call.reject(HttpStatusCode.Gone, LocalAuthError.PIN_EXPIRED)
                return
            }

            PairingSession.Rejection.EXHAUSTED -> {
                call.reject(HttpStatusCode.TooManyRequests, LocalAuthError.TOO_MANY_ATTEMPTS)
                return
            }

            null -> Unit
        }

        onState(LocalAuthServerState.Transferring)

        val token = runSuspendCatching {
            val dto = call.receive<SessionTransferDto>()
            LocalAuthCrypto.decrypt(dto.encryptedToken, dto.iv, dto.salt, session.pin)
        }.getOrElse {
            onState(session.onWrongPin(handle.port))
            call.reject(HttpStatusCode.BadRequest, LocalAuthError.INVALID_PIN)
            return
        }

        runSuspendCatching {
            accountRepository.signInWithToken(token)
        }.getOrElse {
            onState(LocalAuthServerState.Error(LocalAuthError.SIGN_IN_FAILED))
            call.reject(HttpStatusCode.BadGateway, LocalAuthError.SIGN_IN_FAILED)
            return
        }

        // Отвечаем до смены состояния: по Success подписчик гасит сервер.
        call.respond(SessionTransferOkDto())
        onState(LocalAuthServerState.Success)
    }

    /** Неверный PIN восстановим: код остаётся прежним, пока не кончились попытки. */
    private fun PairingSession.onWrongPin(port: Int): LocalAuthServerState {
        val left = registerFailure()
        return if (left == 0) {
            LocalAuthServerState.Error(LocalAuthError.TOO_MANY_ATTEMPTS)
        } else {
            LocalAuthServerState.Pairing(
                pin = pin,
                port = port,
                serviceName = serviceName,
                attemptsLeft = left,
                lastError = LocalAuthError.INVALID_PIN,
            )
        }
    }

    private suspend fun io.ktor.server.application.ApplicationCall.reject(
        status: HttpStatusCode,
        reason: LocalAuthError,
    ) = respond(status, SessionTransferErrorDto(reason.name))

    /** Живой сервер: знает свой порт и умеет идемпотентно останавливаться. */
    internal class Handle {
        private lateinit var engine: EmbeddedServer<*, *>

        @Volatile
        private var stopped = false

        var port: Int = 0
            private set

        suspend fun attach(engine: EmbeddedServer<*, *>) {
            this.engine = engine
            port = engine.engine.resolvedConnectors().first().port
        }

        fun stop() {
            synchronized(this) {
                if (stopped) return
                stopped = true
            }
            runCatching { engine.stop(STOP_GRACE_MS, STOP_TIMEOUT_MS) }
        }
    }

    private companion object {
        const val ANY_FREE_PORT = 0
        const val STOP_GRACE_MS = 500L
        const val STOP_TIMEOUT_MS = 1_500L
    }
}
