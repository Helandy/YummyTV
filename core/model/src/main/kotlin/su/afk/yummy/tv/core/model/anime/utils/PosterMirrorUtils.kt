package su.afk.yummy.tv.core.model.anime.utils

private const val POSTER_MIRROR_HOST = "static.afk.su"
private val YANI_MEGA_POSTER = Regex("""^https?://static\.yani\.tv/posters/mega/(\d+\.avif(?:\?.*)?)$""", RegexOption.IGNORE_CASE)
private val MIRROR_MEGA_POSTER = Regex("""^https?://${Regex.escape(POSTER_MIRROR_HOST)}/posters/mega/\d+\.avif(?:\?.*)?$""", RegexOption.IGNORE_CASE)

/**
 * Ссылка на копию постера yani на нашем хосте: тот же путь и `?v=…`, другой домен.
 * Копия есть только у AVIF-версии `mega` (`static.yani.tv/posters/mega/<id>.avif`), для остальных ссылок `null`.
 */
fun posterMirrorUrl(yaniUrl: String): String? =
    YANI_MEGA_POSTER.find(yaniUrl)?.let { "https://$POSTER_MIRROR_HOST/posters/mega/${it.groupValues[1]}" }

/** Это ссылка на копию постера на нашем хосте (а не на сам yani). */
fun isPosterMirrorUrl(url: String): Boolean = MIRROR_MEGA_POSTER.matches(url)
