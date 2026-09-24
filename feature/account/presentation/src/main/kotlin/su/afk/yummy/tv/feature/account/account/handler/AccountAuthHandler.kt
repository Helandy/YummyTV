package su.afk.yummy.tv.feature.account.account.handler

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.LoginException
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.SyncVideoWatchesUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.feature.account.utils.AccountLoginCredentials
import javax.inject.Inject

/** Wraps account authentication actions and maps domain failures to account-screen results. */
internal class AccountAuthHandler @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshAccountUseCase: RefreshAccountUseCase,
    private val syncVideoWatches: SyncVideoWatchesUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val watchProgressRepository: WatchProgressRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val errorHandler: ErrorHandler,
) {
    suspend fun login(
        credentials: AccountLoginCredentials,
        captchaResponse: String?,
    ): AccountLoginResult =
        runSuspendCatching {
            loginUseCase(credentials.login, credentials.password, captchaResponse)
        }.fold(
            onSuccess = { account ->
                syncLocalWatchesAfterLogin()
                refreshHomeFeedAfterLogin()
                AccountLoginResult.Success(account)
            },
            onFailure = { error ->
                when {
                    error is AccountCaptchaRequiredException ->
                        AccountLoginResult.CaptchaRequired(rejected = captchaResponse != null)

                    error is LoginException -> AccountLoginResult.Failure(error.message)
                    else -> AccountLoginResult.Failure(describeUnexpectedLoginFailure(error))
                }
            },
        )

    /**
     * Причина падения входа за пределами серверного отказа: сеть, Keystore, база, парсинг.
     * Раньше всё это схлопывалось в безликое "Не удалось войти", из-за чего на кастомных
     * прошивках было не понять, что именно ломается.
     */
    private fun describeUnexpectedLoginFailure(error: Throwable): String {
        analyticsTracker.reportError("Unexpected login failure", error, LOGIN_FAILURE_GROUP)
        val parsed = errorHandler.parse(error)
        val technicalName = error::class.simpleName
        return if (error.isNetworkError() || technicalName.isNullOrBlank()) {
            parsed.message
        } else {
            "${parsed.message} ($technicalName)"
        }
    }

    suspend fun logout(): Boolean = runSuspendCatching { logoutUseCase() }.isSuccess

    suspend fun refreshProfile(): AccountRefreshResult =
        runSuspendCatching { refreshAccountUseCase() }.fold(
            onSuccess = { account -> AccountRefreshResult.Success(account) },
            onFailure = { AccountRefreshResult.Failure },
        )

    private suspend fun syncLocalWatchesAfterLogin() {
        runSuspendCatching {
            val videos = watchProgressRepository
                .allMeaningfulVideoProgress()
                .map {
                    VideoWatchSyncItem(
                        videoId = it.videoId,
                        timeSeconds = (it.positionMs / 1000L).toInt(),
                        dateSeconds = (it.updatedAt / 1000L).toInt(),
                    )
                }
            if (!syncVideoWatches(videos)) {
                analyticsTracker.log(TAG) { "Post-login local watch sync returned false" }
            }
        }.onFailure { error ->
            analyticsTracker.log(TAG, error) { "Post-login local watch sync failed" }
        }
    }

    private suspend fun refreshHomeFeedAfterLogin() {
        runSuspendCatching {
            refreshHomeFeed()
        }.onFailure { error ->
            analyticsTracker.log(TAG, error) { "Post-login home feed refresh failed" }
        }
    }

    private companion object {
        const val TAG = "AccountAuthHandler"
        const val LOGIN_FAILURE_GROUP = "login_failure"
    }
}

/** Outcome of a login attempt, including captcha-specific failure state. */
internal sealed interface AccountLoginResult {
    data class Success(val account: YaniAccount) : AccountLoginResult
    data class CaptchaRequired(val rejected: Boolean) : AccountLoginResult
    data class Failure(val message: String? = null) : AccountLoginResult
}

/** Outcome of refreshing the stored account session. */
internal sealed interface AccountRefreshResult {
    data class Success(val account: YaniAccount?) : AccountRefreshResult
    data object Failure : AccountRefreshResult
}
