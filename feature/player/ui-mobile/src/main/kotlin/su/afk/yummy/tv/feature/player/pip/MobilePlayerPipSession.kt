package su.afk.yummy.tv.feature.player.pip

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Rational

internal class MobilePlayerPipSession(
    private val canEnterPictureInPicture: (Context) -> Boolean,
    private val isInPictureInPictureMode: () -> Boolean,
    private val paramsFactory: MobilePlayerPipParamsFactory = MobilePlayerPipParamsFactory(),
) {
    private val defaultAspectRatio = Rational(16, 9)

    @Volatile
    private var active = false

    @Volatile
    private var playing = false

    @Volatile
    private var enabled = true

    @Volatile
    private var aspectRatio: Rational = defaultAspectRatio

    @Volatile
    private var pictureInPictureRequested = false

    @Volatile
    private var callbacks: MobilePlayerPipCallbacks? = null

    fun activate() {
        active = true
    }

    fun release() {
        active = false
        playing = false
        enabled = true
        aspectRatio = defaultAspectRatio
        pictureInPictureRequested = false
        callbacks = null
    }

    fun setPlaying(isPlaying: Boolean, activity: Activity? = null) {
        playing = isPlaying
        if (activity != null && isInPictureInPictureMode()) {
            updateParams(activity)
        }
    }

    fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
        if (!isEnabled) pictureInPictureRequested = false
    }

    fun setCallbacks(callbacks: MobilePlayerPipCallbacks?) {
        this.callbacks = callbacks
    }

    fun setAspectRatio(width: Int, height: Int) {
        aspectRatio = if (width > 0 && height > 0) {
            Rational(width, height).normalizedForPip()
        } else {
            defaultAspectRatio
        }
    }

    fun onPictureInPictureModeChanged() {
        pictureInPictureRequested = false
    }

    fun shouldKeepPlayingOnPause(): Boolean =
        active && enabled && (pictureInPictureRequested || isInPictureInPictureMode())

    fun enterIfPlaying(activity: Activity): Boolean {
        if (!active || !playing) return false
        return enter(activity)
    }

    fun enter(activity: Activity): Boolean {
        if (!enabled ||
            !canEnterPictureInPicture(activity) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        ) {
            return false
        }
        pictureInPictureRequested = true
        return runCatching {
            activity.enterPictureInPictureMode(paramsFactory.build(activity, aspectRatio, playing))
        }.getOrDefault(false).also { entered ->
            if (!entered) pictureInPictureRequested = false
        }
    }

    fun handleAction(action: String?) {
        val currentCallbacks = callbacks ?: return
        when (action) {
            MobilePlayerPipController.ACTION_SEEK_BACKWARD -> currentCallbacks.onSeekBackward()
            MobilePlayerPipController.ACTION_PLAY_PAUSE -> currentCallbacks.onPlayPause()
            MobilePlayerPipController.ACTION_SEEK_FORWARD -> currentCallbacks.onSeekForward()
        }
    }

    private fun updateParams(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            canEnterPictureInPicture(activity)
        ) {
            activity.setPictureInPictureParams(
                paramsFactory.build(
                    activity = activity,
                    aspectRatio = aspectRatio,
                    playing = playing,
                )
            )
        }
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
