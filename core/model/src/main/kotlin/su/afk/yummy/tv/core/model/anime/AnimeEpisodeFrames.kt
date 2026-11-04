package su.afk.yummy.tv.core.model.anime

/**
 * Кадры серий для ряда «Скриншоты» деталки: [tmdbStillUrls] — кадры серии 1 из TMDB. Скриншоты Yani (второй источник)
 * приходят вместе с деталями тайтла. Пусто, если источник ничего не дал.
 */
data class AnimeEpisodeFrames(
    val tmdbStillUrls: List<String> = emptyList(),
)
