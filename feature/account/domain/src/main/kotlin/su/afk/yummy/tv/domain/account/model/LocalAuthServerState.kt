package su.afk.yummy.tv.domain.account.model

/**
 * State of the local authentication server on TV.
 */
sealed interface LocalAuthServerState {
    data object Idle : LocalAuthServerState

    /**
     * Server is running and waiting for discovery/pairing.
     * @property pin Код сопряжения из [LocalAuthCode.LENGTH] символов, показанный пользователю.
     * @property port The port the server is listening on.
     * @property serviceName Имя NSD-сервиса, под которым ТВ реально зарегистрирован (NSD может
     * переименовать его при конфликте); кладётся в QR, чтобы телефон нашёл именно этот ТВ.
     * @property attemptsLeft Сколько неудачных попыток ещё допустимо с этим PIN.
     * @property lastError Ошибка предыдущей попытки, если она была; PIN при этом остаётся прежним.
     */
    data class Pairing(
        val pin: String,
        val port: Int,
        val serviceName: String,
        val attemptsLeft: Int,
        val lastError: LocalAuthError? = null,
    ) : LocalAuthServerState

    /**
     * Session transfer is in progress.
     */
    data object Transferring : LocalAuthServerState

    /**
     * Session successfully received.
     */
    data object Success : LocalAuthServerState

    /**
     * Сопряжение с этим PIN больше невозможно — нужен новый код.
     */
    data class Error(val reason: LocalAuthError) : LocalAuthServerState
}
