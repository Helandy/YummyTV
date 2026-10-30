package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerSourceBalancer

/** Имена источников каталога, у которых плеер (Ashdi, Fenix) отдельный: id из `source://<id>/…` → название для показа. */
private val AGGREGATOR_SOURCE_NAMES = mapOf("mikai" to "Mikai", "animeon" to "AnimeON")

/** «Ashdi» → «Ashdi (Mikai)», если [iframeUrl] — ссылка источника-агрегатора; иначе имя без изменений. */
internal fun String.withExtraSourceName(iframeUrl: String): String {
    if (isBlank()) return this
    val sourceId = iframeUrl.takeIf { it.startsWith("source://", ignoreCase = true) }
        ?.removePrefix("source://")
        ?.substringBefore('/')
        ?.lowercase()
    val sourceName = AGGREGATOR_SOURCE_NAMES[sourceId] ?: return this
    return "$this ($sourceName)"
}

/** Плеер целиком состоит из ссылок источников каталога (`source://…`), то есть это внешний источник, а не балансер. */
internal fun PlayerSourceBalancer.isExtraSourceBalancer(): Boolean {
    val urls = dubbings.flatMap { dubbing -> dubbing.episodes.map { it.iframeUrl } }
    return urls.isNotEmpty() && urls.all { it.startsWith("source://", ignoreCase = true) }
}
