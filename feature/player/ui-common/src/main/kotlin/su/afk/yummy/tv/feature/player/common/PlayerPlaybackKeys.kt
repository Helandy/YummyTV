package su.afk.yummy.tv.feature.player.common

/**
 * Стабильный ключ воспроизведения: url + retryKey + отсортированные заголовки.
 * [offlineCacheKeySegment] добавляется вторым сегментом только если передан (TV);
 * mobile-ключ его не содержит — это влияет на пере-срабатывание media-item эффекта.
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
