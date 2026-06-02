package su.afk.yummy.tv.core.utils

import android.os.Build

object PlatformCapabilities {
    val supportsAndroid7RestrictedMode: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    val supportsSchedule: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val supportsInAppUpdateInstall: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val supportsTvHomeIntegration: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
