package su.afk.yummy.tv.feature.player.mobile.model

import androidx.media3.cast.Cast

/** Имя подключённого Cast-устройства, или null вне активной сессии. */
internal data class MobileCastConnectionState(val deviceName: String?) {
    val isCasting: Boolean get() = deviceName != null
}
