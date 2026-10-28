package su.afk.yummy.tv.source.animeon.data.dto

import kotlinx.serialization.Serializable

/** `GET /api/player/{title}/episodes`: страница серий плеера. */
@Serializable
internal data class AnimeOnEpisodesDto(
    val episodes: List<AnimeOnEpisodeDto> = emptyList(),
)

@Serializable
internal data class AnimeOnEpisodeDto(
    val id: Int,
    val episode: Int = 0,
)

/** `GET /api/player/{episodeId}/episode`: ссылка на страницу плеера и то, что сайт на ней выбрал (у Fenix — озвучку). */
@Serializable
internal data class AnimeOnVideoUrlDto(
    val videoUrl: String? = null,
    val selection: AnimeOnSelectionDto? = null,
)

@Serializable
internal data class AnimeOnSelectionDto(
    val track: String? = null,
)
