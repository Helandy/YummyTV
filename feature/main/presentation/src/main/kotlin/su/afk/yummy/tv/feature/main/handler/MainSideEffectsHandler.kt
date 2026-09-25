package su.afk.yummy.tv.feature.main.handler

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.featuretoggle.api.VersionSupportChecker
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.RestoreAccountSessionUseCase
import su.afk.yummy.tv.domain.update.usecase.GetLatestAppReleaseUseCase
import su.afk.yummy.tv.domain.update.util.isVersionNewer
import su.afk.yummy.tv.feature.main.utils.firstOrZero
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/** Checks for app updates and refreshes the account session. */
internal class MainSideEffectsHandler @Inject constructor(
    private val getLatestAppRelease: GetLatestAppReleaseUseCase,
    private val versionSupportChecker: VersionSupportChecker,
    private val getAccountSession: GetAccountSessionUseCase,
    private val refreshAccount: RefreshAccountUseCase,
    private val restoreAccountSession: RestoreAccountSessionUseCase,
    private val settingsStore: SettingsStore,
    @param:Named("appVersionName") private val versionName: String,
) {
    suspend fun checkForUpdates(): MainUpdateCheckResult =
        runSuspendCatching {
            val isCurrentVersionSupported = versionSupportChecker.isCurrentVersionSupported()
            val release = withTimeoutOrNull(GITHUB_UPDATE_TIMEOUT) {
                getLatestAppRelease(versionName, includePrerelease = settingsStore.betaUpdatesEnabled.first())
            } ?: return@runSuspendCatching MainUpdateCheckResult.NotAvailable
            if (!isCurrentVersionSupported || isVersionNewer(versionName, release.version)) {
                MainUpdateCheckResult.Available(
                    version = release.version,
                    apkUrl = release.apkUrl,
                    changelog = release.changelog,
                    required = !isCurrentVersionSupported,
                    updatesCount = release.updatesCount,
                    isPrerelease = release.isPrerelease,
                )
            } else {
                MainUpdateCheckResult.NotAvailable
            }
        }.getOrDefault(MainUpdateCheckResult.NotAvailable)

    suspend fun restoreAccountIfMissing() {
        runSuspendCatching { restoreAccountSession() }
    }

    suspend fun refreshAccountIfStale(maxAge: kotlin.time.Duration = FORTY_EIGHT_HOURS) {
        if (!getAccountSession().isAuthorized) return
        val refreshedAt = settingsStore.yaniTokenRefreshAt.firstOrZero()
        val ageMs = System.currentTimeMillis() - refreshedAt
        if (ageMs > maxAge.inWholeMilliseconds) {
            runSuspendCatching { refreshAccount() }
        }
    }

    private companion object {
        val GITHUB_UPDATE_TIMEOUT = 5.seconds
        val FORTY_EIGHT_HOURS = 48.hours
    }
}

/** Outcome of checking GitHub for a newer or non-EOL-supported app version. */
internal sealed interface MainUpdateCheckResult {
    data class Available(
        val version: String,
        val apkUrl: String,
        val changelog: String,
        val required: Boolean,
        val updatesCount: Int,
        val isPrerelease: Boolean,
    ) : MainUpdateCheckResult

    data object NotAvailable : MainUpdateCheckResult
}
