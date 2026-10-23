package su.afk.yummy.tv.source.amedia.data.mapper

import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.amedia.data.dto.AmediaLookupRequestDto
import su.afk.yummy.tv.source.amedia.data.dto.AmediaTitleResponseDto
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle

internal object AmediaMapper {
    private const val MAX_OTHER_TITLES = 20
    private const val SITE_PREFIX = "https://amd.online/"

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
        return AmediaTitle(url)
    }
}
