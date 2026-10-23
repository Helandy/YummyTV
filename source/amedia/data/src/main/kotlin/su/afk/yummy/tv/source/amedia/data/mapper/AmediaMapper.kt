package su.afk.yummy.tv.source.amedia.data.mapper

import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.amedia.data.dto.AmediaEpisodeDto
import su.afk.yummy.tv.source.amedia.data.dto.AmediaLookupRequestDto
import su.afk.yummy.tv.source.amedia.data.dto.AmediaTitleResponseDto
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle

internal object AmediaMapper {
    private const val MAX_OTHER_TITLES = 20
    private const val SITE_PREFIX = "https://amd.online/"
    private const val RUTUBE_EMBED_PREFIX = "https://rutube.ru/play/embed/"

    fun toRequest(lookup: AmediaLookup): AmediaLookupRequestDto = AmediaLookupRequestDto(
        malId = lookup.malId,
        title = lookup.title,
        otherTitles = prioritizeAnimeTitles(lookup.title, lookup.otherTitles, MAX_OTHER_TITLES),
        year = lookup.year,
        type = lookup.type,
        episodes = lookup.episodes,
    )

    /** `null`, если сервер не нашёл тайтл или адрес страницы не с amd.online: чужой хост из ответа клиент не читает. */
    fun toTitle(dto: AmediaTitleResponseDto): AmediaTitle? {
        if (!dto.available) return null
        val url = dto.pageUrl?.trim()?.takeIf { it.startsWith(SITE_PREFIX) } ?: return null
        return AmediaTitle(url, dto.episodes.orEmpty().mapNotNull(::toEpisode))
    }

    private fun toEpisode(dto: AmediaEpisodeDto): AmediaEpisode? {
        val source = when {
            dto.vodId != null && dto.vodId > 0 -> AmediaEpisodeSource.Vod(dto.vodId)
            dto.rutubeUrl?.startsWith(RUTUBE_EMBED_PREFIX) == true && dto.rutubeUrl.length > RUTUBE_EMBED_PREFIX.length ->
                AmediaEpisodeSource.Rutube(dto.rutubeUrl)
            else -> return null
        }
        return dto.number.takeIf { it > 0 }?.let { AmediaEpisode(it, source) }
    }
}
