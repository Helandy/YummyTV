package su.afk.yummy.tv.feature.player.pip

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MobilePlayerPipController {
    const val ACTION_SEEK_BACKWARD = "su.afk.yummy.tv.feature.player.pip.SEEK_BACKWARD"
    const val ACTION_PLAY_PAUSE = "su.afk.yummy.tv.feature.player.pip.PLAY_PAUSE"
    const val ACTION_SEEK_FORWARD = "su.afk.yummy.tv.feature.player.pip.SEEK_FORWARD"

    @Volatile
    private var currentSession: MobilePlayerPipSession? = null

    var isInPictureInPictureMode by mutableStateOf(false)
        private set

    internal fun createSession(): MobilePlayerPipSession =
        MobilePlayerPipSession(
            canEnterPictureInPicture = ::canEnter,
            isInPictureInPictureMode = { isInPictureInPictureMode },
        )

    internal fun registerSession(session: MobilePlayerPipSession) {
        currentSession?.takeUnless { it === session }?.release()
        currentSession = session
        session.activate()
    }

    internal fun unregisterSession(session: MobilePlayerPipSession) {
        if (currentSession === session) {
            currentSession = null
        }
        session.release()
    }

    fun updatePictureInPictureMode(enabled: Boolean) {
        isInPictureInPictureMode = enabled
        currentSession?.onPictureInPictureModeChanged()
    }

    fun shouldKeepPlayingOnPause(): Boolean =
        currentSession?.shouldKeepPlayingOnPause() == true

    fun canEnter(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun enterIfPlaying(activity: Activity): Boolean =
        currentSession?.enterIfPlaying(activity) == true

    fun enter(activity: Activity): Boolean =
        currentSession?.enter(activity) == true

    fun handleAction(action: String?) {
        currentSession?.handleAction(action)
    }

    fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}
