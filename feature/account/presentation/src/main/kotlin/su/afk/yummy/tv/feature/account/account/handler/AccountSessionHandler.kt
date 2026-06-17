package su.afk.yummy.tv.feature.account.account.handler

import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.utils.AccountLoginCredentials
import javax.inject.Inject

internal class AccountSessionHandler @Inject constructor(
    private val authHandler: AccountAuthHandler,
) {
    private var loadedUserId: Int = 0
    private var isAuthorized = false
    private var missingProfileRefreshAttempted = false
    private var isMissingProfileRefreshRunning = false

    fun onSessionSnapshot(isAuthorized: Boolean) {
        this.isAuthorized = isAuthorized
        if (!isAuthorized) resetTransientState()
    }

    fun isAuthorized(): Boolean = isAuthorized

    fun beginMissingProfileRecoveryIfNeeded(state: AccountState.State): Boolean {
        if (!isAuthorized) return false
        if (state.userId > 0) return false
        if (missingProfileRefreshAttempted || isMissingProfileRefreshRunning) return false

        missingProfileRefreshAttempted = true
        isMissingProfileRefreshRunning = true
        return true
    }

    fun completeMissingProfileRecovery() {
        isMissingProfileRefreshRunning = false
    }

    fun markProfileChanged() {
        loadedUserId = 0
        missingProfileRefreshAttempted = false
        isMissingProfileRefreshRunning = false
    }

    fun markHubLoadIfNeeded(state: AccountState.State, force: Boolean): Boolean {
        if (!state.isSignedIn || state.userId <= 0) return false
        if (!force && loadedUserId == state.userId) return false
        loadedUserId = state.userId
        return true
    }

    suspend fun login(
        credentials: AccountLoginCredentials,
        captchaResponse: String?,
    ): AccountLoginResult =
        authHandler.login(credentials, captchaResponse)

    suspend fun logout(): Boolean =
        authHandler.logout().also { success ->
            if (success) resetSession()
        }

    suspend fun refreshProfile(): AccountRefreshResult =
        authHandler.refreshProfile()

    private fun resetSession() {
        isAuthorized = false
        resetTransientState()
    }

    private fun resetTransientState() {
        loadedUserId = 0
        missingProfileRefreshAttempted = false
        isMissingProfileRefreshRunning = false
    }
}
