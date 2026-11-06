package su.afk.yummy.tv.core.network.yani

/**
 * Значение `Accept` для картинок в запросах к API yani: `image/avif`, если устройство его декодирует. Без него yani
 * подменяет картинки на JPEG (в `mega` приходит `posters/full`). WebP не просим сознательно: его версии мелкие
 * (до 250x350) и на ТВ заметно портят качество, поэтому у нас только AVIF `mega` или полный JPEG `fullsize`.
 * `null` — AVIF не поддерживается, заголовок не нужен.
 */
internal fun yaniImageAccept(avifSupported: Boolean): String? = "image/avif".takeIf { avifSupported }
