package su.afk.yummy.tv.core.model.anime.utils

/** Настоящий хост статики yani. Других у него нет, поэтому любые варианты приводим к нему. */
private const val YANI_STATIC_HOST = "static.yani.tv"

/** `static-ru.yani.tv`, `static2.yani.tv` и т.п.: поддомен `static…` у `yani.tv`, который не совпадает с настоящим. */
private val YANI_STATIC_ALIAS = Regex("""^(https?://)static[^./]+\.yani\.tv(?=[/:?#]|$)""", RegexOption.IGNORE_CASE)

/**
 * Приводит ссылку на статику yani к `static.yani.tv`. Нужна для ссылок, у которых хост (`static-ru.yani.tv`
 * не резолвится): такие осели в локальном кэше у части пользователей, и без исправления постеры не грузятся.
 * Чужие и уже правильные ссылки возвращаются как есть.
 */
fun normalizeYaniStaticUrl(url: String): String =
    url.replace(YANI_STATIC_ALIAS) { "${it.groupValues[1]}$YANI_STATIC_HOST" }

/** Постер на любом поддомене `yani.tv`, кроме API: путь `/posters/...` и хвост (`?v=…`) переносятся на другой хост как есть. */
private val YANI_POSTER_ON_SUBDOMAIN = Regex(
    """^https?://([a-z0-9-]+)\.yani\.tv(/posters/[^?#]+(?:\?.*)?)$""",
    RegexOption.IGNORE_CASE,
)

private const val YANI_API_SUBDOMAIN = "api"

/** Поддомен статики: сам [YANI_STATIC_HOST] без суффикса `.yani.tv`. */
private const val YANI_STATIC_SUBDOMAIN = "static"

/**
 * Тот же постер на `static.yani.tv`: запасной источник на случай, когда yani отдаёт ссылку на другой хост картинок
 * (`imgproxy.yani.tv` вместо `static.yani.tv`), а он недоступен. Хост не угадываем по имени: подходит любой поддомен
 * `yani.tv`, кроме API. `null` для ссылки, которая уже ведёт на `static.yani.tv`, и для чужих хостов.
 */
fun yaniStaticPosterFallback(url: String): String? {
    val match = YANI_POSTER_ON_SUBDOMAIN.find(url) ?: return null
    val subdomain = match.groupValues[1]
    if (subdomain.equals(YANI_API_SUBDOMAIN, ignoreCase = true) ||
        subdomain.equals(YANI_STATIC_SUBDOMAIN, ignoreCase = true)
    ) {
        return null
    }
    return "https://$YANI_STATIC_HOST${match.groupValues[2]}"
}
