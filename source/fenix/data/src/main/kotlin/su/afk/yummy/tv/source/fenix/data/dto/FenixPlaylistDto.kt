package su.afk.yummy.tv.source.fenix.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `window.FENIX_PLAYLIST` страницы плеера: серии тайтла (на `/vod/` — одна серия). Новые страницы отдают только индекс серий
 * (без `hls_master_url` и `tracks`) и [playback] — токен, с которым потоки серии запрашиваются отдельно, по [slug].
 */
@Serializable
internal data class FenixPlaylistDto(
    val slug: String? = null,
    @SerialName("default_edition") val defaultEdition: String? = null,
    val episodes: List<FenixEpisodeDto> = emptyList(),
    val playback: FenixPlaybackDto? = null,
)

/** Доступ к `GET /api/playback/{slug}/episodes/{edition}/{номер}`: уходит заголовком `X-Fenix-Playback`. */
@Serializable
internal data class FenixPlaybackDto(
    val token: String? = null,
)

/** Ответ `GET /api/playback/{slug}/episodes/{edition}/{номер}`: серия с потоками и озвучками. */
@Serializable
internal data class FenixEpisodeDetailDto(
    val episode: FenixEpisodeDto? = null,
)

@Serializable
internal data class FenixEpisodeDto(
    val number: Int = 0,
    val edition: String? = null,
    @SerialName("hls_master_url") val hlsMasterUrl: String? = null,
    val tracks: List<FenixTrackDto> = emptyList(),
)

/** Озвучка серии: у каждой свой HLS-мастер, в котором звучит только она. */
@Serializable
internal data class FenixTrackDto(
    val label: String? = null,
    @SerialName("hls_url") val hlsUrl: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("vod_url") val vodUrl: String? = null,
)
