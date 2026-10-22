package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import su.afk.yummy.tv.core.designsystem.locals.LocalNowMillis
import su.afk.yummy.tv.core.model.seasonal.seasonalThemeAt

/** Переопределение сезона (для превью и отладки); `null` — определять по дате. */
val LocalSeasonalThemeOverride: ProvidableCompositionLocal<SeasonalTheme?> =
    staticCompositionLocalOf { null }

/** Включено ли праздничное оформление в настройках пользователя. */
val LocalSeasonalDecorationsEnabled: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { true }

/** Текущая праздничная тема: по доверенным часам приложения, а не по часам устройства. */
@Composable
fun rememberSeasonalTheme(): SeasonalTheme {
    if (!LocalSeasonalDecorationsEnabled.current) return SeasonalTheme.NONE
    val override = LocalSeasonalThemeOverride.current
    val nowMillis = LocalNowMillis.current
    return remember(override) { override ?: seasonalThemeAt(nowMillis()) }
}
