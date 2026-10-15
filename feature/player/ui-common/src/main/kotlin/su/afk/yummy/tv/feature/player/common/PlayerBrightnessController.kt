package su.afk.yummy.tv.feature.player.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Яркость мобильного плеера, выбранная пользователем жестом: доля 0.01–1.0 или `null`,
 * пока яркость не менялась и действует системная.
 *
 * Синглтон на весь процесс: восстанавливается из DataStore при старте и сохраняется обратно
 * (с дебаунсом), чтобы переживать выход из плеера и перезапуск приложения.
 */
@OptIn(FlowPreview::class)
@Singleton
class PlayerBrightnessController @Inject constructor(
    settingsStore: PlayerSettingsStore,
    @DefaultApplicationScope private val scope: CoroutineScope,
) {
    private val _brightness = MutableStateFlow<Float?>(null)

    /** Сохранённая яркость как доля 0.01f..1f; `null` — использовать системную. */
    val brightness: StateFlow<Float?> = _brightness.asStateFlow()

    init {
        scope.launch {
            val storedPercent = settingsStore.mobilePlayerBrightnessPercent.first()
            // Жест мог успеть сработать раньше, чем DataStore ответил: его значение главнее.
            if (_brightness.value == null && storedPercent != null) {
                _brightness.value = (storedPercent / 100f).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
            }
        }
        scope.launch {
            _brightness
                .filterNotNull()
                .map { (it * 100f).roundToInt() }
                .distinctUntilChanged()
                .debounce(PERSIST_DEBOUNCE_MS)
                .collect { percent -> settingsStore.setMobilePlayerBrightnessPercent(percent) }
        }
    }

    fun setFraction(value: Float) {
        _brightness.value = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    companion object {
        const val MIN_BRIGHTNESS = 0.01f
        const val MAX_BRIGHTNESS = 1f
        private const val PERSIST_DEBOUNCE_MS = 500L
    }
}
