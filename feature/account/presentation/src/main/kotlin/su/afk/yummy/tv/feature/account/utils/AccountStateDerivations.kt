package su.afk.yummy.tv.feature.account.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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

/** Decrements the counter for [type] by one, used to reflect a notification mutation optimistically. */
internal fun ImmutableList<NotificationCount>.decrementCount(type: String): ImmutableList<NotificationCount> =
    map { if (it.type == type && it.count > 0) it.copy(count = it.count - 1) else it }.toImmutableList()
