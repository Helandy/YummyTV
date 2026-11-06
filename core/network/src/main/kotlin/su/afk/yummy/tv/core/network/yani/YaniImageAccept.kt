package su.afk.yummy.tv.core.network.yani

/**
 * Значение `Accept` для картинок в запросах к API yani: поддерживаемые устройством `image/avif` и `image/webp`
 * через запятую. Без них yani подменяет картинки на JPEG худшего качества (в `mega` приходит `posters/full`).
 * `null` — устройство не умеет ни того, ни другого, заголовок не нужен.
 */
internal fun yaniImageAccept(avifSupported: Boolean, webpSupported: Boolean): String? =
    listOfNotNull(
        "image/avif".takeIf { avifSupported },
        "image/webp".takeIf { webpSupported },
    ).takeIf { it.isNotEmpty() }?.joinToString(",")
