package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import su.afk.yummy.tv.core.model.settings.BrowserUserAgentProfile
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отдаёт User-Agent выбранного в настройках профиля синхронно: сетевой код читает его в момент
 * запроса и ждать DataStore не может.
 *
 * До первого чтения DataStore значение — профиль по умолчанию. Это безопасно: все профили из
 * [BrowserUserAgentProfile] принимаются CDN, а расхождение возможно лишь у самого первого запроса
 * после холодного старта.
 */
@Singleton
internal class DataStoreBrowserUserAgentProvider @Inject constructor(
    settingsStore: PlayerSettingsStore,
    @IoApplicationScope scope: CoroutineScope,
) : BrowserUserAgentProvider {

    private val profile: StateFlow<BrowserUserAgentProfile> = settingsStore.browserUserAgentProfile
        .stateIn(scope, SharingStarted.Eagerly, BrowserUserAgentProfile.DEFAULT)

    override val userAgent: String get() = profile.value.userAgent
}
