package su.afk.yummy.tv.source.anime4k.data.mapper

import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTicketResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTitleResponseDto
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kEpisode
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kFile
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTicket
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle

internal object Anime4kMapper {

    /** `null`, если сервер сказал, что 4K нет, или не прислал ни одной серии с озвучкой. */
    fun toTitle(malId: Int, dto: Anime4kTitleResponseDto): Anime4kTitle? {
        if (!dto.available) return null
        val episodes = dto.episodes
            .map { Anime4kEpisode(number = it.number, season = it.season, dubbings = it.dubbings.filter(String::isNotBlank)) }
            .filter { it.number.isNotBlank() && it.dubbings.isNotEmpty() }
        return episodes.takeIf { it.isNotEmpty() }?.let { Anime4kTitle(malId = malId, episodes = it) }
    }

    fun toTicket(dto: Anime4kTicketResponseDto): Anime4kTicket {
        require(dto.ticket.isNotBlank()) { "Сервер не выдал тикет" }
        return Anime4kTicket(value = dto.ticket, notBeforeSec = dto.notBeforeSec, expiresInSec = dto.expiresInSec)
    }

    fun toStreams(dto: Anime4kStreamsResponseDto): Anime4kStreams {
        val files = dto.files
            .filter { it.url.isNotBlank() && it.q > 0 }
            .map { Anime4kFile(quality = it.q, bitrate = it.br, url = it.url) }
            .sortedByDescending { it.quality }
        require(files.isNotEmpty()) { "Сервер не прислал ни одной ссылки" }
        return Anime4kStreams(referer = dto.referer, files = files)
    }
}
