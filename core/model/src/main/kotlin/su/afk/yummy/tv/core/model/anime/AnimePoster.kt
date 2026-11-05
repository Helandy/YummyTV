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
 * Ссылка на постер ровно той версии, что выбрана в настройках [quality], без запасных версий.
 * Пусто, если у постера этой версии нет.
 */
fun AnimePoster?.urlsByQuality(quality: PosterQuality): List<String> {
    if (this == null) return emptyList()
    val selected = when (quality) {
        PosterQuality.LOW -> medium
        PosterQuality.STANDARD -> big
        PosterQuality.MEGA -> mega
        PosterQuality.HIGH -> fullsize
    }
    return listOfNotNull(selected?.takeIf { it.isNotBlank() })
}

/** Желаемая для выбранного [quality] ссылка на постер (первая из [urlsByQuality]). */
fun AnimePoster?.urlFor(quality: PosterQuality): String? = urlsByQuality(quality).firstOrNull()

/**
 * Лучшая доступная ссылка на постер: от самой крупной версии к самой мелкой. Только для мест, где
 * ссылка сохраняется строкой в кэш/хранилище и настройки качества недоступны; в UI — [urlFor].
 */
fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
