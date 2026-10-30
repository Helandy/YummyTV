package su.afk.yummy.tv.data.details.mapper

/**
 * Кадры серий из Tenrai («номер серии → ссылка») по порядку номеров, первые [limit]: у Tenrai один кадр на серию.
 * Серии без числового номера идут в конец.
 */
internal fun Map<String, String>.toOrderedFrameUrls(limit: Int): List<String> =
    entries
        .sortedBy { (episode, _) -> episode.toIntOrNull() ?: Int.MAX_VALUE }
        .take(limit)
        .map { (_, url) -> url }
