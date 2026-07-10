package su.afk.yummy.tv.feature.player.common.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import su.afk.yummy.tv.feature.player.common.PlayerLoadControlFactory
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerMediaSessionService : MediaSessionService() {
    @Inject
    internal lateinit var playbackConfig: PlayerPlaybackConfig

    @Inject
    internal lateinit var allohaSessionManager: AllohaPlaybackSessionManager

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val exoPlayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(playbackConfig.dataSourceFactory()))
            .setLoadControl(PlayerLoadControlFactory.create())
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(object : Player.Listener {
            private var overriddenAudioGroup: TrackGroup? = null

            override fun onTracksChanged(tracks: Tracks) {
                val selection = playbackConfig.trackSelectionConfig()
                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (selection.audioTrackPolicy != PlayerAudioTrackPolicy.FirstAudioGroup ||
                    audioGroups.isEmpty()
                ) {
                    clearAllohaAudioOverride(trackSelector)
                    return
                }
                val firstAudioGroup = audioGroups.first().mediaTrackGroup
                if (overriddenAudioGroup == firstAudioGroup) return
                overriddenAudioGroup = firstAudioGroup
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(ALLOHA_AUDIO_LANGUAGE)
                        .setPreferredTextLanguage(ALLOHA_AUDIO_LANGUAGE)
                        .setPreferredVideoMimeType(MimeTypes.VIDEO_H264)
                        .setRendererDisabled(AUDIO_RENDERER_INDEX, false)
                        .setOverrideForType(TrackSelectionOverride(firstAudioGroup, 0))
                        .build()
                )
                Log.i(
                    LOG_TAG,
                    "Alloha audio selected groups=${audioGroups.size} " +
                            "tracksInFirstGroup=${firstAudioGroup.length} group=0 track=0 " +
                            "offline=${selection.isOfflinePlayback}",
                )
            }

            private fun clearAllohaAudioOverride(trackSelector: DefaultTrackSelector) {
                if (overriddenAudioGroup == null) return
                overriddenAudioGroup = null
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(null)
                        .setPreferredTextLanguage(null)
                        .setPreferredVideoMimeType(null)
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .build()
                )
                Log.i(LOG_TAG, "Alloha audio override cleared")
            }
        })
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(createSessionActivityPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player ?: return
        if (!current.playWhenReady || current.mediaItemCount == 0 || current.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        allohaSessionManager.closeActive()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun createSessionActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_SESSION_ACTIVITY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val LOG_TAG = "PlayerMediaSession"
        const val ALLOHA_AUDIO_LANGUAGE = "ru"
        const val AUDIO_RENDERER_INDEX = 1
        const val REQUEST_CODE_SESSION_ACTIVITY = 40_101
    }
}
