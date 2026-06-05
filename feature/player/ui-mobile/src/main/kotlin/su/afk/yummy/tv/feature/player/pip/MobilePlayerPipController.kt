package su.afk.yummy.tv.feature.player.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MobilePlayerPipController {
    const val ACTION_PLAY_PAUSE = "su.afk.yummy.tv.feature.player.pip.PLAY_PAUSE"

    private val defaultAspectRatio = Rational(16, 9)

    @Volatile
    private var active = false

    @Volatile
    private var playing = false

    @Volatile
    private var aspectRatio: Rational = defaultAspectRatio

    @Volatile
    private var pictureInPictureRequested = false

    @Volatile
    private var onPlayPause: (() -> Unit)? = null

    var isInPictureInPictureMode by mutableStateOf(false)
        private set

    fun setPlayerActive(isActive: Boolean) {
        active = isActive
        if (!isActive) {
            playing = false
            aspectRatio = defaultAspectRatio
            pictureInPictureRequested = false
            onPlayPause = null
        }
    }

    fun setPlaying(isPlaying: Boolean, activity: Activity? = null) {
        playing = isPlaying
        if (activity != null && isInPictureInPictureMode) {
            updateParams(activity)
        }
    }

    fun setPlayPauseAction(action: (() -> Unit)?) {
        onPlayPause = action
    }

    fun setAspectRatio(width: Int, height: Int) {
        aspectRatio = if (width > 0 && height > 0) {
            Rational(width, height).normalizedForPip()
        } else {
            defaultAspectRatio
        }
    }

    fun updatePictureInPictureMode(enabled: Boolean) {
        isInPictureInPictureMode = enabled
        pictureInPictureRequested = false
    }

    fun shouldKeepPlayingOnPause(): Boolean =
        active && (pictureInPictureRequested || isInPictureInPictureMode)

    fun canEnter(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun enterIfPlaying(activity: Activity): Boolean {
        if (!active || !playing) return false
        return enter(activity)
    }

    fun enter(activity: Activity): Boolean {
        if (!canEnter(activity) || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        pictureInPictureRequested = true
        return runCatching {
            activity.enterPictureInPictureMode(buildParams(activity))
        }.getOrDefault(false).also { entered ->
            if (!entered) pictureInPictureRequested = false
        }
    }

    fun handleAction(action: String?) {
        if (action == ACTION_PLAY_PAUSE) {
            onPlayPause?.invoke()
        }
    }

    private fun updateParams(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && canEnter(activity)) {
            activity.setPictureInPictureParams(buildParams(activity))
        }
    }

    private fun buildParams(activity: Activity): PictureInPictureParams =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(listOf(playPauseAction(activity)))
                .build()
        } else {
            error("Picture-in-picture params require Android O+")
        }

    private fun playPauseAction(activity: Activity): RemoteAction {
        val intent = Intent(activity, MobilePlayerPipActionReceiver::class.java)
            .setAction(ACTION_PLAY_PAUSE)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, flags)
        val label = if (playing) "Пауза" else "Играть"
        val iconRes =
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        return RemoteAction(
            Icon.createWithResource(activity, iconRes),
            label,
            label,
            pendingIntent,
        )
    }

    fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}

private fun Rational.normalizedForPip(): Rational {
    val value = numerator.toFloat() / denominator.toFloat()
    return when {
        value > 2.39f -> Rational(239, 100)
        value < 1f / 2.39f -> Rational(100, 239)
        else -> this
    }
}
