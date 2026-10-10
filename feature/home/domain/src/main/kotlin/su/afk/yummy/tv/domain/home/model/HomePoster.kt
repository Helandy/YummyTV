package su.afk.yummy.tv.domain.home.model

data class HomePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

/** Лучшая доступная ссылка на постер: от самой крупной версии к самой мелкой. */
fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
