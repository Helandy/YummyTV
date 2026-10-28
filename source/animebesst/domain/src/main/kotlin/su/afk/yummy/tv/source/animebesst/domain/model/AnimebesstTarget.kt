package su.afk.yummy.tv.source.animebesst.domain.model

/**
 * Адрес серии в источнике: slug страницы тайтла (`957-kikansha-no-mahou-wa-tokubetsu-desu-2nd-season`), номер серии и хосты сайта
 * в порядке пробы: основной и зеркала (сайт для части сетей закрыт, зеркала отдают те же страницы).
 */
data class AnimebesstTarget(
    val slug: String,
    val episode: Int,
    val hosts: List<String>,
)
