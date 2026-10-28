package su.afk.yummy.tv.source.fenix.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `window.FENIX_PLAYLIST` страницы плеера: серии тайтла (на `/vod/` — одна серия). */
@Serializable
internal data class FenixPlaylistDto(
    @SerialName("default_edition") val defaultEdition: String? = null,
    val episodes: List<FenixEpisodeDto> = emptyList(),
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
