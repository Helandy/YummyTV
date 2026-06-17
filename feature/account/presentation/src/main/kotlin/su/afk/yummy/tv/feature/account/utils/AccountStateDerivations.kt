package su.afk.yummy.tv.feature.account.utils

import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.feature.account.account.AccountState

internal data class AccountLoginCredentials(
    val login: String,
    val password: String,
)

internal fun AccountState.State.loginCredentialsOrNull(): AccountLoginCredentials? {
    val loginValue = login.trim()
    if (loginValue.isBlank() || password.isBlank()) return null
    return AccountLoginCredentials(
        login = loginValue,
        password = password,
    )
}

internal fun List<NotificationCount>.totalUnreadCount(): Int =
    sumOf { it.count }
