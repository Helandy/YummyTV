package su.afk.yummy.tv.source.moon.domain.model

/**
 * Поток серии на Moon: [url] — HLS-мастер или файл, [headers] — заголовки, без которых CDN отвечает 403 (Referer страницы плеера,
 * Origin сайта), [qualities] — карта качеств по возрастанию (лучшее последнее), если страница отдаёт список файлов; `null` — поток
 * играет адаптивно.
 */
data class MoonStreams(
    val url: String,
    val headers: Map<String, String>,
    val qualities: Map<String, String>? = null,
)
