package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupDto
import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupEpisodeDto

/** Ключ — номер серии строкой; серии без пометок отбрасываются. */
internal fun EpisodeFlagsBackupDto.toEpisodeFlagsByNumber(): Map<String, EpisodeFlags> =
    episodes.asSequence()
        .filter { it.number > 0 && (it.filler || it.recap) }
        .associate { it.number.toString() to EpisodeFlags(filler = it.filler, recap = it.recap) }

/** Документ кэша из пометок внешнего источника: кэш и потребители не зависят от того, откуда пришли пометки. */
internal fun Map<String, EpisodeFlags>.toEpisodeFlagsDocument(malId: Int): EpisodeFlagsBackupDto = EpisodeFlagsBackupDto(
    malId = malId.toLong(),
    episodes = mapNotNull { (number, flags) ->
        number.toIntOrNull()?.let { EpisodeFlagsBackupEpisodeDto(number = it, filler = flags.filler, recap = flags.recap) }
    },
)
