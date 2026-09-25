package su.afk.yummy.tv.feature.account.utils

import su.afk.yummy.tv.feature.account.account.model.AccountUiError

private const val MIN_PASSWORD_LENGTH = 6
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Проверяет форму регистрации до запроса; `null` — всё заполнено корректно. */
internal fun validateRegistrationForm(
    email: String,
    username: String,
    password: String,
): AccountUiError? = when {
    email.isBlank() || username.isBlank() || password.isBlank() -> AccountUiError.CREDENTIALS_REQUIRED
    !EMAIL_REGEX.matches(email) -> AccountUiError.INVALID_EMAIL
    password.length < MIN_PASSWORD_LENGTH -> AccountUiError.PASSWORD_TOO_SHORT
    else -> null
}
