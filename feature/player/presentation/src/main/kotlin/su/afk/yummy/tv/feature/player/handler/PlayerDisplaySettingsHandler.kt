package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import javax.inject.Inject

/**
 * Настройки картинки (TV-размер/зум и мобильный transform), хранящиеся на пару тайтл/плеер.
 *
 * Owns scoped resize/transform subscriptions and debounced persistence jobs.
 */
internal class PlayerDisplaySettingsHandler @Inject constructor(
    private val settings: PlayerSettingsHandler,
    private val sourceSelection: PlayerSourceSelectionHandler,
) {
    private var resizeJob: Job? = null
    private var activeResizeScope: PlayerResizeSettingsScope? = null
    private var mobileTransformJob: Job? = null
    private var mobileTransformSaveJob: Job? = null
    private var activeMobileTransformScope: PlayerResizeSettingsScope? = null

    /**
     * Подписывается на настройки для текущей пары тайтл/плеер.
     *
     * Пока сохранённое значение нового scope не пришло, показываются дефолты, а не настройки
     * прошлого плеера. [force] переподписывает и при том же scope (новый destination).
     */
    fun observeActive(host: PlayerStateHost, force: Boolean = false) {
        observeResize(host, force)
        observeMobileTransform(host, force)
    }

    fun selectResizeMode(host: PlayerStateHost, mode: PlayerResizeMode) {
        val value = PlayerResizeSettings(
            resizeMode = mode,
            zoomLevel = host.state.zoomLevel,
        )
        host.update { copy(resizeMode = value.resizeMode) }
        saveResize(host, value)
    }

    fun selectZoomLevel(host: PlayerStateHost, level: PlayerZoomLevel) {
        val value = PlayerResizeSettings(
            resizeMode = PlayerResizeMode.ZOOM,
            zoomLevel = level,
        )
        host.update { copy(resizeMode = value.resizeMode, zoomLevel = value.zoomLevel) }
        saveResize(host, value)
    }

    fun changeMobileTransform(host: PlayerStateHost, value: PlayerMobileVideoTransformSettings) {
        host.update {
            copy(
                mobileVideoScale = value.scale,
                mobileVideoOffsetX = value.offsetX,
                mobileVideoOffsetY = value.offsetY,
            )
        }
        val scope = activeScope(host)
        mobileTransformSaveJob?.cancel()
        mobileTransformSaveJob = host.scope.launch {
            settings.saveMobileVideoTransformSettings(scope, value)
        }
    }

    private fun observeResize(host: PlayerStateHost, force: Boolean) {
        val scope = activeScope(host)
        if (!force && scope == activeResizeScope) return
        activeResizeScope = scope
        resizeJob?.cancel()
        resizeJob = settings.observeResizeSettings(scope)
            .onEach { value ->
                if (scope == activeResizeScope) {
                    host.update { copy(resizeMode = value.resizeMode, zoomLevel = value.zoomLevel) }
                }
            }
            .launchIn(host.scope)
        host.update {
            copy(
                resizeMode = PlayerResizeMode.FIT,
                zoomLevel = PlayerZoomLevel.PERCENT_10,
            )
        }
    }

    private fun observeMobileTransform(host: PlayerStateHost, force: Boolean) {
        val scope = activeScope(host)
        if (!force && scope == activeMobileTransformScope) return
        activeMobileTransformScope = scope
        mobileTransformJob?.cancel()
        mobileTransformSaveJob?.cancel()
        mobileTransformJob = settings.observeMobileVideoTransformSettings(scope)
            .onEach { value ->
                if (scope == activeMobileTransformScope) {
                    host.update {
                        copy(
                            mobileVideoScale = value.scale,
                            mobileVideoOffsetX = value.offsetX,
                            mobileVideoOffsetY = value.offsetY,
                        )
                    }
                }
            }
            .launchIn(host.scope)
        host.update {
            copy(
                mobileVideoScale = 1f,
                mobileVideoOffsetX = 0f,
                mobileVideoOffsetY = 0f,
            )
        }
    }

    private fun saveResize(host: PlayerStateHost, value: PlayerResizeSettings) {
        val scope = activeScope(host)
        host.scope.launch { settings.saveResizeSettings(scope, value) }
    }

    /** Ключ хранения, общий для TV-настроек размера и мобильного transform. */
    private fun activeScope(host: PlayerStateHost): PlayerResizeSettingsScope =
        sourceSelection.resizeSettingsScope(host.state)
}
