package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupDto
import su.afk.yummy.tv.data.details.dto.MalEpisodeDto
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto

/** Резерв приводится к документу Tenrai, чтобы кэш и потребители флагов не зависели от источника; картинок у резерва нет. */
internal fun EpisodeFlagsBackupDto.toMalEpisodesDto(): MalEpisodesDto = MalEpisodesDto(
    data = episodes.map { MalEpisodeDto(episodeNumber = it.number, filler = it.filler, recap = it.recap) },
)
