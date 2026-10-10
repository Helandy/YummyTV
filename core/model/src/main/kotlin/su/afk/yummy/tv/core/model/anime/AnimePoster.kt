package su.afk.yummy.tv.core.model.anime

data class AnimePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

/** Лучшая доступная ссылка на постер: от самой крупной версии к самой мелкой. */
fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
