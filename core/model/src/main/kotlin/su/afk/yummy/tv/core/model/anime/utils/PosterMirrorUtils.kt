package su.afk.yummy.tv.core.model.anime.utils

/**
 * Хост своего сервера со статикой. Настоящий домен в git не хранится: приложение задаёт его при старте из
 * `local.properties` (`yummytv.staticHost`), по умолчанию заглушка.
 */
object PosterMirrorConfig {
    @Volatile
    var host: String = "static.self.made"
}

private val YANI_MEGA_POSTER = Regex("""^https?://static\.yani\.tv/posters/mega/(\d+\.avif(?:\?.*)?)$""", RegexOption.IGNORE_CASE)

/**
 * Ссылка на копию постера yani на нашем хосте: тот же путь и `?v=…`, другой домен.
 * Копия есть только у AVIF-версии `mega` (`static.yani.tv/posters/mega/<id>.avif`), для остальных ссылок `null`.
 */
fun posterMirrorUrl(yaniUrl: String): String? =
    YANI_MEGA_POSTER.find(yaniUrl)?.let { "https://${PosterMirrorConfig.host}/posters/mega/${it.groupValues[1]}" }

/** Это ссылка на копию постера на нашем хосте (а не на сам yani). */
fun isPosterMirrorUrl(url: String): Boolean =
    Regex("""^https?://${Regex.escape(PosterMirrorConfig.host)}/posters/mega/\d+\.avif(?:\?.*)?$""", RegexOption.IGNORE_CASE).matches(url)
