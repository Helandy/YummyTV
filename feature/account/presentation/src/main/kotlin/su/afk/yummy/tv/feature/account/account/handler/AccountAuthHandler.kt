package su.afk.yummy.tv.feature.account.account.handler

import su.afk.yummy.tv.core.logger.AppLogger
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.SyncVideoWatchesUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.feature.account.utils.AccountLoginCredentials
import javax.inject.Inject

/** Wraps account authentication actions and maps domain failures to account-screen results. */
internal class AccountAuthHandler @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshAccountUseCase: RefreshAccountUseCase,
    private val syncVideoWatches: SyncVideoWatchesUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val watchProgressStore: WatchProgressStore,
) {
    suspend fun login(
        credentials: AccountLoginCredentials,
        captchaResponse: String?,
    ): AccountLoginResult =
        runCatching {
            loginUseCase(credentials.login, credentials.password, captchaResponse)
        }.fold(
            onSuccess = { account ->
                syncLocalWatchesAfterLogin()
                refreshHomeFeedAfterLogin()
                AccountLoginResult.Success(account)
            },
            onFailure = { error ->
                if (error is AccountCaptchaRequiredException) {
                    AccountLoginResult.CaptchaRequired(rejected = captchaResponse != null)
                } else {
                    AccountLoginResult.Failure
                }
            },
        )

    suspend fun logout(): Boolean = runCatching { logoutUseCase() }.isSuccess

    suspend fun refreshProfile(): AccountRefreshResult =
        runCatching { refreshAccountUseCase() }.fold(
            onSuccess = { account -> AccountRefreshResult.Success(account) },
            onFailure = { AccountRefreshResult.Failure },
        )

    private suspend fun syncLocalWatchesAfterLogin() {
        runCatching {
            val videos = watchProgressStore
                .allMeaningfulVideoProgress()
                .map {
                    VideoWatchSyncItem(
                        videoId = it.videoId,
                        timeSeconds = (it.positionMs / 1000L).toInt(),
                        dateSeconds = (it.updatedAt / 1000L).toInt(),
                    )
                }
            if (!syncVideoWatches(videos)) {
                AppLogger.w(TAG) { "Post-login local watch sync returned false" }
            }
        }.onFailure { error ->
            AppLogger.w(TAG, error) { "Post-login local watch sync failed" }
        }
    }

    private suspend fun refreshHomeFeedAfterLogin() {
        runCatching {
            refreshHomeFeed()
        }.onFailure { error ->
            AppLogger.w(TAG, error) { "Post-login home feed refresh failed" }
        }
    }

    private companion object {
        const val TAG = "AccountAuthHandler"
    }
}

/** Outcome of a login attempt, including captcha-specific failure state. */
internal sealed interface AccountLoginResult {
    data class Success(val account: YaniAccount) : AccountLoginResult
    data class CaptchaRequired(val rejected: Boolean) : AccountLoginResult
    data object Failure : AccountLoginResult
}

/** Outcome of refreshing the stored account session. */
internal sealed interface AccountRefreshResult {
    data class Success(val account: YaniAccount?) : AccountRefreshResult
    data object Failure : AccountRefreshResult
}
