package su.afk.yummy.tv.feature.home.handler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.feature.home.model.HomeAnnouncement
import su.afk.yummy.tv.feature.home.model.HomeAnnouncementResult
import javax.inject.Inject

/** Читает серверное объявление и строку-сообщение из фичефлагов, помнит прочитанное. */
internal class HomeAnnouncementHandler @Inject constructor(
    private val settingsStore: SettingsStore,
    private val featureToggleProvider: FeatureToggleProvider,
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver,
    private val analyticsTracker: AnalyticsTracker,
) {
    /** Каждое обновление фичефлагов после старта экрана. */
    fun updates(): Flow<Unit> {
        val initialActivationId = featureToggleUpdateObserver.currentActivationId
        return featureToggleUpdateObserver.updates
            .filter { activationId -> activationId > initialActivationId }
            .map { }
    }

    /** Доп. строка-сообщение с сервера; `null` — не показывается. */
    fun statusMessage(): String? =
        featureToggleProvider.getString(FeatureFlags.statusMessage).trim().ifBlank { null }

    /** Берёт объявление из фичефлагов: показывать его или нет. */
    suspend fun resolve(): HomeAnnouncementResult {
        val id = featureToggleProvider.getString(FeatureFlags.announcementId).trim()
        val message = featureToggleProvider.getString(FeatureFlags.announcementMessage).trim()
        // Пустой id/сообщение или id == "0" означают, что объявление выключено.
        if (id.isBlank() || id == "0" || message.isBlank()) {
            analyticsTracker.log(TAG_ANNOUNCEMENT) {
                "Skipped: id='$id' message.isBlank=${message.isBlank()}"
            }
            return HomeAnnouncementResult.Disabled
        }
        val lastSeenId = settingsStore.lastSeenAnnouncementId.first()
        if (id == lastSeenId) {
            analyticsTracker.log(TAG_ANNOUNCEMENT) { "Skipped: id='$id' already seen" }
            return HomeAnnouncementResult.AlreadySeen
        }
        val title = featureToggleProvider.getString(FeatureFlags.announcementTitle).trim()
        val button = featureToggleProvider.getString(FeatureFlags.announcementButton).trim()
        analyticsTracker.log(TAG_ANNOUNCEMENT) {
            "Showing: id='$id' lastSeenId='$lastSeenId' title.isBlank=${title.isBlank()} button.isBlank=${button.isBlank()}"
        }
        return HomeAnnouncementResult.Show(
            HomeAnnouncement(
                id = id,
                title = title.ifBlank { null },
                message = message,
                buttonText = button.ifBlank { null },
            ),
        )
    }

    suspend fun markSeen(id: String) = settingsStore.markAnnouncementSeen(id)

    private companion object {
        const val TAG_ANNOUNCEMENT = "HomeAnnouncement"
    }
}
