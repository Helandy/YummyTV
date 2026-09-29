package su.afk.yummy.tv.feature.player.common

/**
 * Стабильный ключ воспроизведения: url + retryKey + отсортированные заголовки.
 * [offlineCacheKeySegment] (ключ офлайн-кэша и выбранные субтитры) добавляется вторым сегментом.
 */
fun buildPlayerPlaybackKey(
    url: String,
    retryKey: Int,
    headers: Map<String, String>,
    offlineCacheKeySegment: String? = null,
): String = buildString {
    append(url)
    if (offlineCacheKeySegment != null) {
        append('|').append(offlineCacheKeySegment)
    }
    append('|').append(retryKey)
    headers.entries.sortedBy { it.key.lowercase() }
        .forEach { (key, value) -> append('|').append(key).append('=').append(value) }
}
