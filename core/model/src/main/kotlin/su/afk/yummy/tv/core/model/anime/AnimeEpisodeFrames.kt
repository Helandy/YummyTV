package su.afk.yummy.tv.core.model.anime

/**
 * Кадры серий для ряда «Скриншоты» деталки: [tmdbStillUrls] — кадры серии 1 из TMDB, [tmdbStillMirrorUrls] — их копии
 * на нашем хосте (позиция в позицию, пустая строка — копии нет). Скриншоты Yani (второй источник)
 * приходят вместе с деталями тайтла. Пусто, если источник ничего не дал.
 */
data class AnimeEpisodeFrames(
    val tmdbStillUrls: List<String> = emptyList(),
    val tmdbStillMirrorUrls: List<String> = emptyList(),
)
