package su.afk.yummy.tv.android.seasonal

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import su.afk.yummy.tv.core.model.seasonal.seasonalThemeAt
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import su.afk.yummy.tv.core.utils.system.ProcessLifecycleCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Подменяет иконку приложения в лаунчере на праздничную: в манифесте три `activity-alias`
 * (обычная, Хэллоуин, Новый год), включён ровно один. Следит за настройкой «Праздничное
 * оформление» и датой при каждом запуске процесса.
 *
 * Применяет смену только когда приложение уходит в фон: отключение алиаса, через который запущена
 * текущая задача, закрывает её, и пользователь вылетал бы на домашний экран сразу после старта.
 */
@Singleton
class SeasonalAppIconSwitcher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val appClock: AppClock,
    @DefaultApplicationScope private val scope: CoroutineScope,
) : ProcessLifecycleCoordinator() {

    @Volatile
    private var desiredTheme: SeasonalTheme? = null

    override fun start() {
        super.start()
        scope.launch {
            runSuspendCatching {
                settingsStore.seasonalDecorationsEnabled
                    .map { enabled -> if (enabled) seasonalThemeAt(appClock.nowMillis()) else SeasonalTheme.NONE }
                    .distinctUntilChanged()
                    .collect { theme -> desiredTheme = theme }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        desiredTheme?.let(::apply)
    }

    private fun apply(theme: SeasonalTheme) {
        val packageManager = context.packageManager
        ALIASES.forEach { (aliasTheme, aliasName) ->
            val component = ComponentName(context, aliasName)
            val wanted = aliasTheme == theme
            // Манифестное состояние алиаса учитывается через DEFAULT, поэтому сверяем с ним.
            val current = packageManager.getComponentEnabledSetting(component)
            val isEnabled = when (current) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> aliasTheme == SeasonalTheme.NONE
                else -> false
            }
            if (isEnabled == wanted) return@forEach
            packageManager.setComponentEnabledSetting(
                component,
                if (wanted) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private companion object {
        const val PACKAGE = "su.afk.yummy.tv.android.launcher"
        val ALIASES = listOf(
            SeasonalTheme.NONE to "$PACKAGE.DefaultLauncherAlias",
            SeasonalTheme.HALLOWEEN to "$PACKAGE.HalloweenLauncherAlias",
            SeasonalTheme.NEW_YEAR to "$PACKAGE.NewYearLauncherAlias",
        )
    }
}
