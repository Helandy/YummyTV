package su.afk.yummy.tv.feature.player.mobile.cast

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import su.afk.yummy.tv.core.utils.cast.CastSupport

/** Имя подключённого Cast-устройства, или null вне активной сессии. */
internal data class MobileCastConnectionState(val deviceName: String?) {
    val isCasting: Boolean get() = deviceName != null
}

/**
 * Отслеживает активную Cast-сессию процесса напрямую через [Cast]/[CastSession] - в отличие от
 * [androidx.media3.common.Player.getDeviceInfo] это даёт человекочитаемое имя устройства
 * ([CastSession.getCastDevice]), которое ContentFrame/RemoteCastPlayer не прокидывают.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun rememberMobileCastConnectionState(): MobileCastConnectionState {
    val context = LocalContext.current
    var deviceName by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        // Без Cast слушать нечего, а на старых GMS любое обращение к media3-Cast роняет процесс
        // (см. CastSupport).
        if (!CastSupport.isSupported(context)) return@DisposableEffect onDispose { }
        val cast = Cast.getSingletonInstance(context)

        fun applySession(session: CastSession?) {
            deviceName = session?.takeIf { it.isConnected }?.castDevice?.friendlyName
        }

        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) =
                applySession(session)

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) =
                applySession(session)

            override fun onSessionEnded(session: CastSession, error: Int) = applySession(null)
            override fun onSessionSuspended(session: CastSession, reason: Int) = applySession(null)
            override fun onSessionStarting(session: CastSession) = Unit
            override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
            override fun onSessionEnding(session: CastSession) = Unit
            override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
            override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
        }

        cast.addSessionManagerListener(listener)
        applySession(cast.getCurrentCastSession())

        onDispose { cast.removeSessionManagerListener(listener) }
    }
    return MobileCastConnectionState(deviceName)
}

@OptIn(UnstableApi::class)
internal fun stopCasting(context: Context) {
    if (!CastSupport.isSupported(context)) return
    Cast.getSingletonInstance(context).endCurrentSession(true)
}
