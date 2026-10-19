package su.afk.yummy.tv.source.anilibria.data.utils

private const val GENERIC_HOST = "cache.libria.fun"
private const val RU_HOST = "cache-rfn.libria.fun"

/**
 * API выбирает узел CDN по стране клиента, и общий `cache.libria.fun` отдаёт сегменты для EU,
 * а российский `cache-rfn.libria.fun`
 * Ссылки других узлов не трогаем.
 */
internal fun String.preferRuCdnHost(): String =
    if (startsWith("https://$GENERIC_HOST/")) replaceFirst(GENERIC_HOST, RU_HOST) else this
