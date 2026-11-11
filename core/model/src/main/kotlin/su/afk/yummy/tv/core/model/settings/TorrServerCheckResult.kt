package su.afk.yummy.tv.core.model.settings

/** Итог проверки адреса TorrServer из настроек. */
sealed interface TorrServerCheckResult {
    /** Сервер ответил; [version] — его версия (например, `MatriX.150`), может быть пустой. */
    data class Ok(val version: String) : TorrServerCheckResult

    /** Сервер не отвечает: неверный адрес, выключен или нет доступа к локальной сети. */
    data object Unreachable : TorrServerCheckResult

    /** Сервер ответил отказом авторизации: нужны логин и пароль или они неверны. */
    data object Unauthorized : TorrServerCheckResult

    /** Сервер ответил кодом [httpCode], которого мы не ждём (например, адрес указывает не на TorrServer). */
    data class UnexpectedResponse(val httpCode: Int) : TorrServerCheckResult
}
