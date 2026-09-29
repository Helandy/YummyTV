package su.afk.yummy.tv.feature.player.common.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_SERVICE_LOG_TAG

/**
 * Для источников с политикой [PlayerAudioTrackPolicy.FirstAudioGroup] (Alloha) принудительно
 * выбирает первую аудиогруппу потока и H.264; для остальных снимает подмену.
 */
@OptIn(UnstableApi::class)
internal class PlayerAllohaAudioOverride(
    private val trackSelector: DefaultTrackSelector,
    private val playbackConfig: PlayerPlaybackConfig,
    private val analyticsTracker: AnalyticsTracker,
) : Player.Listener {
    private var overriddenAudioGroup: TrackGroup? = null

    override fun onTracksChanged(tracks: Tracks) {
        val selection = playbackConfig.trackSelectionConfig()
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (selection.audioTrackPolicy != PlayerAudioTrackPolicy.FirstAudioGroup ||
            audioGroups.isEmpty()
        ) {
            clearOverride()
            return
        }
        val firstAudioGroup = audioGroups.first().mediaTrackGroup
        if (overriddenAudioGroup == firstAudioGroup) return
        overriddenAudioGroup = firstAudioGroup
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setPreferredAudioLanguage(ALLOHA_AUDIO_LANGUAGE)
                .setPreferredVideoMimeType(MimeTypes.VIDEO_H264)
                .setRendererDisabled(AUDIO_RENDERER_INDEX, false)
                .setOverrideForType(TrackSelectionOverride(firstAudioGroup, 0))
                .build(),
        )
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
            "Alloha audio selected groups=${audioGroups.size} " +
                "tracksInFirstGroup=${firstAudioGroup.length} group=0 track=0 " +
                "offline=${selection.isOfflinePlayback}"
        }
    }

    private fun clearOverride() {
        if (overriddenAudioGroup == null) return
        overriddenAudioGroup = null
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setPreferredAudioLanguage(null)
                .setPreferredTextLanguage(null)
                .setPreferredVideoMimeType(null)
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .build(),
        )
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) { "Alloha audio override cleared" }
    }

    private companion object {
        const val ALLOHA_AUDIO_LANGUAGE = "ru"
        const val AUDIO_RENDERER_INDEX = 1
    }
}
