package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.Serializable

/** Ответ резервной ручки YummyTV API `/episode-flags/{malId}`: только серии с пометкой филлер/рекап. */
@Serializable
data class EpisodeFlagsBackupDto(
    val malId: Long = 0,
    val episodes: List<EpisodeFlagsBackupEpisodeDto> = emptyList(),
    val fetchedAt: Long = 0,
)

@Serializable
data class EpisodeFlagsBackupEpisodeDto(
    val number: Int = 0,
    val filler: Boolean = false,
    val recap: Boolean = false,
)
