package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeSettings
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import javax.inject.Inject

/** Owns scoped resize/transform subscriptions and debounced persistence jobs. */
internal class PlayerDisplaySettingsHandler @Inject constructor(
    private val settings: PlayerSettingsHandler,
) {
    private var resizeJob: Job? = null
    private var activeResizeScope: PlayerResizeSettingsScope? = null
    private var mobileTransformJob: Job? = null
    private var mobileTransformSaveJob: Job? = null
    private var activeMobileTransformScope: PlayerResizeSettingsScope? = null

    fun observeResizeSettings(
        scope: PlayerResizeSettingsScope,
        coroutineScope: CoroutineScope,
        force: Boolean = false,
        onChanged: (PlayerResizeSettings) -> Unit,
    ): Boolean {
        if (!force && scope == activeResizeScope) return false
        activeResizeScope = scope
        resizeJob?.cancel()
        resizeJob = settings.observeResizeSettings(scope)
            .onEach { if (scope == activeResizeScope) onChanged(it) }
            .launchIn(coroutineScope)
        return true
    }

    fun observeMobileTransformSettings(
        scope: PlayerResizeSettingsScope,
        coroutineScope: CoroutineScope,
        force: Boolean = false,
        onChanged: (PlayerMobileVideoTransformSettings) -> Unit,
    ): Boolean {
        if (!force && scope == activeMobileTransformScope) return false
        activeMobileTransformScope = scope
        mobileTransformJob?.cancel()
        mobileTransformSaveJob?.cancel()
        mobileTransformJob = settings.observeMobileVideoTransformSettings(scope)
            .onEach { if (scope == activeMobileTransformScope) onChanged(it) }
            .launchIn(coroutineScope)
        return true
    }

    fun saveResizeSettings(
        scope: PlayerResizeSettingsScope,
        value: PlayerResizeSettings,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch { settings.saveResizeSettings(scope, value) }
    }

    fun saveMobileTransformSettings(
        scope: PlayerResizeSettingsScope,
        value: PlayerMobileVideoTransformSettings,
        coroutineScope: CoroutineScope,
    ) {
        mobileTransformSaveJob?.cancel()
        mobileTransformSaveJob = coroutineScope.launch {
            settings.saveMobileVideoTransformSettings(scope, value)
        }
    }
}
