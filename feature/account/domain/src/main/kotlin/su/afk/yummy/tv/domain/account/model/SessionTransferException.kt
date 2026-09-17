package su.afk.yummy.tv.domain.account.model

/**
 * Ошибка передачи сессии по локальной сети.
 *
 * @property reason причина, если её сообщил ТВ; `null` — сетевой сбой или неизвестный ответ.
 */
class SessionTransferException(
    message: String,
    val reason: LocalAuthError? = null,
) : RuntimeException(message)
