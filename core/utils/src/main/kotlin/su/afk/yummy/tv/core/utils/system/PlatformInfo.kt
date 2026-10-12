package su.afk.yummy.tv.core.utils.system

import android.os.Build

/**
 * Версия Android для presentation-слоя без прямого доступа к `android.os.Build`: состояние экранов
 * и аналитика получают значения отсюда, а не импортируют платформенные классы.
 */
object PlatformInfo {
    /** Числовая версия API текущего устройства. */
    val sdkInt: Int get() = Build.VERSION.SDK_INT

    /** Android 9 (API 28) или новее. */
    val isAtLeastPie: Boolean get() = sdkInt >= Build.VERSION_CODES.P
}
