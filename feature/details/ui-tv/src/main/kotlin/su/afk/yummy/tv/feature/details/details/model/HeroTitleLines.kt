package su.afk.yummy.tv.feature.details.details.model

/** Строки заголовка hero: [tail] == null — весь [head] крупным в две строки; иначе построчно, [marquee] — бегущая третья. */
internal data class HeroTitleLines(
    val head: String,
    val tail: String? = null,
    val marquee: String? = null,
)
