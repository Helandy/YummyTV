package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerState

/**
 * The state after switching to the Alloha dubbing [audioId], or null when there is no such dubbing.
 *
 * Every dubbing carries its own ladder, so the switch is a lookup: the ladder becomes the stream's
 * quality map and the URL is picked from it. The chosen quality is kept only if the new dubbing
 * offers it, otherwise the best one plays.
 */
internal fun PlayerState.State.withAllohaAudio(audioId: String): PlayerState.State? {
    val track = allohaAudioTracks.firstOrNull { it.id == audioId && it.qualities.isNotEmpty() }
        ?: return null
    val ladder = track.qualities
    val quality = selectedQuality?.takeIf(ladder::containsKey)
    return copy(
        selectedAllohaAudioId = track.id,
        streamQualityMap = ladder,
        selectedQuality = quality,
        streamUrl = quality?.let(ladder::get) ?: ladder.values.last(),
    )
}
