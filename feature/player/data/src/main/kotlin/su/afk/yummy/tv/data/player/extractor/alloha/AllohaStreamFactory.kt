package su.afk.yummy.tv.data.player.extractor.alloha

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

/**
 * Builds the stream the player starts with from one opened session: Alloha's own default dubbing
 * (or the first), at [preferredQualityLabel] when that dubbing offers it, otherwise at its best
 * quality. Every dubbing's ladder rides along in `allohaAudioTracks`, so switching dubbing later is
 * a lookup in the player state, not another request.
 */
internal fun buildAllohaStream(
    opened: AllohaOpenedSession,
    preferredQualityLabel: String?,
): PlayerStreamResolveResult.Stream {
    val tracks = opened.sources.audioTracks
    val track = checkNotNull(tracks.firstOrNull { it.isDefault } ?: tracks.firstOrNull()) {
        "Alloha session has no dubbing"
    }
    val headers = opened.headers
    return PlayerStreamResolveResult.Stream(
        url = preferredQualityLabel?.let(track.qualities::get) ?: track.qualities.values.last(),
        headers = headers,
        qualities = track.qualities,
        qualityHeaders = track.qualities.keys.associateWith { headers },
        allohaAudioTracks = tracks,
        selectedAllohaAudioId = track.id,
        allohaSubtitles = opened.sources.subtitles,
    )
}
