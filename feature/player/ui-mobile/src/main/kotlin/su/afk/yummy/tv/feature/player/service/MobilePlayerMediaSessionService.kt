package su.afk.yummy.tv.feature.player.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import su.afk.yummy.tv.feature.player.common.PlayerLoadControlFactory
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MobilePlayerMediaSessionService : MediaSessionService() {
    @Inject
    internal lateinit var playbackConfig: MobilePlayerPlaybackConfig

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val exoPlayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(playbackConfig.dataSourceFactory())
            )
            .setLoadControl(PlayerLoadControlFactory.create())
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player ?: return
        if (!currentPlayer.playWhenReady ||
            currentPlayer.mediaItemCount == 0 ||
            currentPlayer.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
