package su.afk.yummy.tv.core.model.anime.utils

/**
 * Хост своего сервера со статикой. Настоящий домен в git не хранится: приложение задаёт его при старте из
 * `local.properties` (`yummytv.staticHost`), по умолчанию заглушка.
 */
object PosterMirrorConfig {
    @Volatile
    var host: String = "static.self.made"
}

/** Постер yani любого вида и формата (`posters/mega/1.avif`, `posters/full/1.jpg`): хост, числовой id и хвост (`?v=…`). */
private val YANI_POSTER = Regex("""^https?://([^/?#]+)/posters/[a-z]+/(\d+)\.[a-z0-9]+(\?.*)?$""", RegexOption.IGNORE_CASE)

/**
 * Ссылка на копию постера yani на нашем хосте: тот же путь и `?v=…`, другой домен.
 * На нашем хосте лежит только AVIF-версия `mega` (`<хост>/posters/mega/<id>.avif`), поэтому и для JPEG (`posters/full/<id>.jpg`)
 * и других видов постера копией служит тот же `<id>` в виде `mega` AVIF. Ссылка не про постер yani (нет `/posters/<вид>/<id>.<ext>`) копии
 * не получает: `null`. Хост исходной ссылки не важен: какой бы адрес ни пришёл от yani (в том числе некоректный), при его недоступности картинка берётся
 * с нашего хоста. Ссылка, которая уже ведёт на наш хост, копии не получает.
 */
fun posterMirrorUrl(yaniUrl: String): String? {
    val match = YANI_POSTER.find(yaniUrl) ?: return null
    if (match.groupValues[1].equals(PosterMirrorConfig.host, ignoreCase = true)) return null
    return "https://${PosterMirrorConfig.host}/posters/mega/${match.groupValues[2]}.avif${match.groupValues[3]}"
}

/** Это ссылка на копию постера на нашем хосте (а не на сам yani). */
fun isPosterMirrorUrl(url: String): Boolean =
    Regex("""^https?://${Regex.escape(PosterMirrorConfig.host)}/posters/mega/\d+\.avif(?:\?.*)?$""", RegexOption.IGNORE_CASE).matches(url)

/**
 * Ключ кэша картинки для постера yani: `yani-poster:<id>`; `null`, если ссылка не про постер. Хост, вид (`mega`/`full`),
 * формат и `?v=…` в ключ не входят. Постер с таким id не меняется (новый постер получает новый id), поэтому одна и та же
 * картинка из любого источника цепочки (API, `static.yani.tv`, наш хост) ложится в кэш под одним ключом: при смене хоста
 * yani или недоступном CDN уже скачанный постер берётся из кэша, а не теряется.
 */
fun yaniPosterCacheKey(url: String): String? =
    YANI_POSTER.find(url)?.let { "yani-poster:${it.groupValues[2]}" }
