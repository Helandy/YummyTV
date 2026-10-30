package su.afk.yummy.tv.source.animeon.domain.model

/**
 * Поток серии AnimeON: [masterUrl] — HLS-мастер или файл, [qualities] — карта качеств по возрастанию (лучшее последнее; пуста, если
 * плеер играет мастер адаптивно), [headers] — заголовки, без которых CDN плеера отвечает 403 (нужны Moon; у Ashdi и Fenix пусты).
 */
data class AnimeOnStreams(
    val masterUrl: String,
    val qualities: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
)
