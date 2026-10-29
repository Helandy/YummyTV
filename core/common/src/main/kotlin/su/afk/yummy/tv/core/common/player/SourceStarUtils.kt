package su.afk.yummy.tv.core.common.player

private val PLAYER_PREFIX = Regex("^плеер\\s+", RegexOption.IGNORE_CASE)

/** Ключ звёздочки озвучки: имя без регистра и лишних пробелов. */
fun String.dubbingStarKey(): String = trim().lowercase()

/** Ключ звёздочки балансера: имя без префикса «Плеер», регистра и лишних пробелов. */
fun String.balancerStarKey(): String = trim().replace(PLAYER_PREFIX, "").trim().lowercase()

fun String.isStarredDubbing(starred: Set<String>): Boolean =
    starred.isNotEmpty() && dubbingStarKey() in starred

fun String.isStarredBalancer(starred: Set<String>): Boolean =
    starred.isNotEmpty() && balancerStarKey() in starred

/** Переставляет элементы с звездой наверх, сохраняя исходный порядок внутри групп. */
fun <T> List<T>.starredFirst(isStarred: (T) -> Boolean): List<T> =
    if (none(isStarred)) this else sortedBy { if (isStarred(it)) 0 else 1 }
