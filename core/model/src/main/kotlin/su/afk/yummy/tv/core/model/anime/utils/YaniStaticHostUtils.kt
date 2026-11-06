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
