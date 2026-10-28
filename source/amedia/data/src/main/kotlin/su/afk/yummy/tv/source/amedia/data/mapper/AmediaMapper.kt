package su.afk.yummy.tv.source.amedia.data.mapper

import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode

internal object AmediaMapper {
    private const val SITE_PREFIX = "https://amd.online/"
    private const val RUTUBE_EMBED_PREFIX = "https://rutube.ru/play/embed/"

    /** `null`, если адрес страницы не с amd.online: чужой хост из ответа клиент не читает. Серии берутся из единственного варианта. */
    fun toTitle(availability: SourceAvailability): AmediaTitle? {
        val url = availability.url.trim().takeIf { it.startsWith(SITE_PREFIX) } ?: return null
        return AmediaTitle(url, availability.variants.firstOrNull()?.episodes.orEmpty().mapNotNull(::toEpisode))
    }

    /** Запись плеера (`id` = vodId) или Rutube-embed (`url`); остальное, как и серии без положительного номера, отбрасывается. */
    private fun toEpisode(episode: SourceEpisode): AmediaEpisode? {
        if (episode.number <= 0) return null
        val vodId = episode.id?.toLongOrNull()?.takeIf { it > 0 }
        val embed = episode.url?.takeIf { it.startsWith(RUTUBE_EMBED_PREFIX) && it.length > RUTUBE_EMBED_PREFIX.length }
        val source = when {
            vodId != null -> AmediaEpisodeSource.Vod(vodId)
            embed != null -> AmediaEpisodeSource.Rutube(embed)
            else -> return null
        }
        return AmediaEpisode(episode.number, source)
    }
}
