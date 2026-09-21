package su.afk.yummy.tv.android.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.core.model.anime.WatchedEpisodeRule
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Держит [WatchedEpisodeRule] в соответствии с пользовательскими порогами "просмотрено".
 * Правило вызывается синхронно из storage/presentation, поэтому настройку зеркалим в процесс,
 * а не передаём параметром. До первой эмиссии действуют дефолты — они же значения DataStore
 * по умолчанию.
 */
@Singleton
class WatchedEpisodeRuleSync @Inject constructor(
    private val settingsStore: PlayerSettingsStore,
    @DefaultApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        settingsStore.watchedThresholds
            .onEach(WatchedEpisodeRule::update)
            .launchIn(scope)
    }
}
