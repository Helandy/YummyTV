package su.afk.yummy.tv.core.model.anime.utils

/**
 * Хост своего сервера со статикой. Настоящий домен в git не хранится: приложение задаёт его при старте из
 * `local.properties` (`yummytv.staticHost`), по умолчанию заглушка.
 */
object PosterMirrorConfig {
    @Volatile
    var host: String = "static.self.made"
}

private val MEGA_POSTER = Regex("""^https?://([^/?#]+)/posters/mega/(\d+\.avif(?:\?.*)?)$""", RegexOption.IGNORE_CASE)

/**
 * Ссылка на копию постера yani на нашем хосте: тот же путь и `?v=…`, другой домен.
 * Копия есть только у AVIF-версии `mega` (`<хост>/posters/mega/<id>.avif`), для остальных ссылок `null`. Хост исходной
 * ссылки не важен: какой бы адрес ни пришёл от yani (в том числе некоректный), при его недоступности картинка берётся
 * с нашего хоста. Ссылка, которая уже ведёт на наш хост, копии не получает.
 */
fun posterMirrorUrl(yaniUrl: String): String? {
    val match = MEGA_POSTER.find(yaniUrl) ?: return null
    if (match.groupValues[1].equals(PosterMirrorConfig.host, ignoreCase = true)) return null
    return "https://${PosterMirrorConfig.host}/posters/mega/${match.groupValues[2]}"
}

/** Это ссылка на копию постера на нашем хосте (а не на сам yani). */
fun isPosterMirrorUrl(url: String): Boolean =
    Regex("""^https?://${Regex.escape(PosterMirrorConfig.host)}/posters/mega/\d+\.avif(?:\?.*)?$""", RegexOption.IGNORE_CASE).matches(url)
