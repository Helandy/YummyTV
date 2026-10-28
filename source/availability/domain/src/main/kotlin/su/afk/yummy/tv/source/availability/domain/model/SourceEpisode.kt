package su.afk.yummy.tv.source.availability.domain.model

/**
 * Серия варианта; длительность и границы опенинга в секундах, если источник их знает. [id] и [url] нужны источникам, у которых
 * серия живёт в своём плеере: [id] — идентификатор записи плеера (у Amedia `vodId` на `aser.pro`), [url] — прямая embed-ссылка,
 * если сайт вставил вместо неё чужой плеер (Rutube).
 */
data class SourceEpisode(
    val number: Int,
    val durationSec: Int?,
    val openingStartSec: Int?,
    val openingEndSec: Int?,
    val id: String? = null,
    val url: String? = null,
)
