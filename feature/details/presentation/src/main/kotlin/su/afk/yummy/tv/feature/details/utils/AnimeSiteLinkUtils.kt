package su.afk.yummy.tv.feature.details.utils

import android.net.Uri

private const val SITE_ORIGIN = "https://ru.yummyani.me"
private const val CATALOG_ITEM_SEGMENT = "catalog"
private const val CATALOG_ITEM_SUBSEGMENT = "item"

/** Хосты сайта yani, ссылки которых приложение умеет открывать (тот же список, что в манифесте). */
private val SITE_HOSTS = setOf("yummyani.me", "ru.yummyani.me", "www.yummyani.me", "yani.tv", "www.yani.tv")

/** Страница тайтла на сайте: `https://ru.yummyani.me/catalog/item/{anime_url}`; без slug — null. */
fun animeSiteUrl(animeUrl: String): String? =
    animeUrl.trim().takeIf { it.isNotEmpty() }
        ?.let { "$SITE_ORIGIN/$CATALOG_ITEM_SEGMENT/$CATALOG_ITEM_SUBSEGMENT/$it" }

/** Slug тайтла из ссылки сайта `https://<хост yani>/catalog/item/{slug}`; для других ссылок — null. */
fun Uri.toAnimeSiteSlug(): String? {
    if (scheme != "https" && scheme != "http") return null
    if (host?.lowercase() !in SITE_HOSTS) return null
    val segments = pathSegments
    if (segments.size < 3) return null
    if (segments[0] != CATALOG_ITEM_SEGMENT || segments[1] != CATALOG_ITEM_SUBSEGMENT) return null
    return segments[2].takeIf { it.isNotBlank() }
}
