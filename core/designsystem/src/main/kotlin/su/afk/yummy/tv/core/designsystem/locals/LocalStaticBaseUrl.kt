package su.afk.yummy.tv.core.designsystem.locals

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Папка статики нашего сервера со слешем в конце (например `https://host/static/`). Выводится из адреса API, который
 * можно заменить удалённо, поэтому задаётся приложением. Пустая = статика недоступна, картинки не показываются.
 */
val LocalStaticBaseUrl = staticCompositionLocalOf { "" }
