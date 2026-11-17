package su.afk.yummy.tv.core.designsystem.locals

import androidx.compose.runtime.staticCompositionLocalOf

/** Приложение сейчас читает каталог и детали из своей копии YummyTV, потому что yani недоступен. */
val LocalYaniMirrorActive = staticCompositionLocalOf { false }
