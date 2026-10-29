package su.afk.yummy.tv.source.anistar.data.mapper

import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability

internal object AnistarMapper {
    private val MIRROR_ORIGIN = Regex("""^(https://[^/?#]+)/""")

    /**
     * `null`, если ответ не годится для воспроизведения: нет положительного id новости или адрес плеер-страницы не https
     * (зеркало берётся из её хоста). Серии берутся из единственного варианта, номера без положительного значения отбрасываются.
     */
    fun toTitle(availability: SourceAvailability): AnistarTitle? {
        val newsId = availability.key.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val mirror = MIRROR_ORIGIN.find(availability.url.trim())?.groupValues?.get(1) ?: return null
        val episodes = availability.variants.firstOrNull()?.episodes.orEmpty().filter { it.number > 0 }.map { AnistarEpisode(it.number) }
        return AnistarTitle(newsId, mirror, episodes)
    }
}
