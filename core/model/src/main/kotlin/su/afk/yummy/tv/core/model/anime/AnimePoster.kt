package su.afk.yummy.tv.core.model.anime

import su.afk.yummy.tv.core.model.settings.PosterQuality

data class AnimePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

/**
 * Все доступные ссылки на постер без дублей, в порядке предпочтения для выбранного [quality]:
 * первая — желаемая версия, остальные — запасные на случай, если она не загрузится.
 */
fun AnimePoster?.urlsByQuality(quality: PosterQuality): List<String> {
    if (this == null) return emptyList()
    val ordered = when (quality) {
        PosterQuality.LOW -> listOf(medium, big, fullsize, small, mega)
        PosterQuality.STANDARD -> listOf(big, medium, fullsize, small, mega)
        PosterQuality.MEGA -> listOf(mega, big, medium, fullsize, small)
        PosterQuality.HIGH -> listOf(fullsize, mega, big, medium, small)
    }
    return ordered.filterNotNull().filter { it.isNotBlank() }.distinct()
}

/** Желаемая для выбранного [quality] ссылка на постер (первая из [urlsByQuality]). */
fun AnimePoster?.urlFor(quality: PosterQuality): String? = urlsByQuality(quality).firstOrNull()

/**
 * Лучшая доступная ссылка на постер: от самой крупной версии к самой мелкой. Только для мест, где
 * ссылка сохраняется строкой в кэш/хранилище и настройки качества недоступны; в UI — [urlFor].
 */
fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
