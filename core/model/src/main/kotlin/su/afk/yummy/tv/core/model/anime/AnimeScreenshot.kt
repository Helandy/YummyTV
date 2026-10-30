package su.afk.yummy.tv.core.model.anime

/**
 * Скриншот тайтла. [fallbackUrls] — запасные ссылки на ту же карточку по приоритету: если [url] не загрузилась
 * (например, CDN TMDB у пользователя заблокирован), экран показывает следующую. [cacheKey] — стабильный ключ кэша
 * картинок карточки: одинаков у всех её ссылок, поэтому картинка из любого источника лежит под одним ключом и при
 * следующем открытии берётся из кэша без сети; `null` — кэшировать по ссылке.
 */
data class AnimeScreenshot(
    val id: Int?,
    val episode: String?,
    val url: String?,
    val fallbackUrls: List<String> = emptyList(),
    val cacheKey: String? = null,
)
