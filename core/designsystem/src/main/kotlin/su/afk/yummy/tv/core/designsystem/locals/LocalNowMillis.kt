package su.afk.yummy.tv.core.designsystem.locals

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Текущее время приложения в epoch-миллисекундах (доверенное, если оно доступно). Системные часы
 * приставок без NTP уезжают на дни, поэтому UI не должен читать `System.currentTimeMillis()`.
 */
val LocalNowMillis = staticCompositionLocalOf<() -> Long> { System::currentTimeMillis }
