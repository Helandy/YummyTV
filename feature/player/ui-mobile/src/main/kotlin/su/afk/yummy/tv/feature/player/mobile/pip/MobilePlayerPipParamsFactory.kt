package su.afk.yummy.tv.feature.player.mobile.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import su.afk.yummy.tv.feature.player.mobile.R

internal class MobilePlayerPipParamsFactory {

    @RequiresApi(Build.VERSION_CODES.O)
    fun build(
        activity: Activity,
        aspectRatio: Rational,
        playing: Boolean,
    ): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(
                listOf(
                    action(
                        activity = activity,
                        action = MobilePlayerPipController.ACTION_SEEK_BACKWARD,
                        requestCode = 0,
                        labelRes = R.string.player_mobile_pip_rewind_10,
                        iconRes = android.R.drawable.ic_media_rew,
                    ),
                    playPauseAction(activity, playing),
                    action(
                        activity = activity,
                        action = MobilePlayerPipController.ACTION_SEEK_FORWARD,
                        requestCode = 2,
                        labelRes = R.string.player_mobile_pip_forward_10,
                        iconRes = android.R.drawable.ic_media_ff,
                    ),
                )
            )
            .build()

    private fun playPauseAction(activity: Activity, playing: Boolean): RemoteAction {
        val label = activity.getString(
            if (playing) R.string.player_mobile_pip_pause else R.string.player_mobile_pip_play,
        )
        val iconRes =
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        return action(
            activity = activity,
            action = MobilePlayerPipController.ACTION_PLAY_PAUSE,
            requestCode = 1,
            label = label,
            iconRes = iconRes,
        )
    }

    private fun action(
        activity: Activity,
        action: String,
        requestCode: Int,
        labelRes: Int,
        iconRes: Int,
    ): RemoteAction =
        action(
            activity = activity,
            action = action,
            requestCode = requestCode,
            label = activity.getString(labelRes),
            iconRes = iconRes,
        )

    private fun action(
        activity: Activity,
        action: String,
        requestCode: Int,
        label: String,
        iconRes: Int,
    ): RemoteAction {
        val intent = Intent(activity, MobilePlayerPipActionReceiver::class.java)
            .setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(activity, requestCode, intent, flags)
        return RemoteAction(
            Icon.createWithResource(activity, iconRes),
            label,
            label,
            pendingIntent,
        )
    }
}
