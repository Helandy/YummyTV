package su.afk.yummy.tv.data.account.utils

private val croppedBannerRegex = Regex("""^(.*/banners/)cropped/([^/?]+)\.webp(\?.*)?$""")

/**
 * Широкая версия баннера, которую показывает сайт: `banners/full/{name}.avif` (панорама ~8:1).
 * API в поле `full` отдаёт тот же cropped-URL, поэтому адрес выводим из cropped.
 * Существует только в AVIF; null, если URL не похож на cropped-баннер.
 */
internal fun String.toWideBannerUrl(): String? =
    croppedBannerRegex.matchEntire(this)?.let { match ->
        val (prefix, name, query) = match.destructured
        "${prefix}full/$name.avif$query"
    }
