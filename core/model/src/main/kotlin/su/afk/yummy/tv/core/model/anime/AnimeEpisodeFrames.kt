package su.afk.yummy.tv.core.model.anime

/**
 * Кадры серий из двух источников для ряда «Скриншоты» деталки: [tmdbStillUrls] — кадры серии 1 из TMDB,
 * [tenraiFrameUrls] — кадры первых серий из Tenrai (по одному на серию, по порядку номеров). Скриншоты Yani
 * (третий источник) приходят вместе с деталями тайтла. Пусто, если источник ничего не дал.
 */
data class AnimeEpisodeFrames(
    val tmdbStillUrls: List<String> = emptyList(),
    val tenraiFrameUrls: List<String> = emptyList(),
)
