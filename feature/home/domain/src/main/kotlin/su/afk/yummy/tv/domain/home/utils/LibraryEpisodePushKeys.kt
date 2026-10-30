package su.afk.yummy.tv.domain.home.utils

/** Ключ серии для учёта уже обработанных локальных пушей. */
fun libraryEpisodeKey(animeId: Int, episodeNumber: Int): String = "$animeId:$episodeNumber"

/**
 * Отметка завершённой базовой линии среди известных ключей. Нужна отдельно от самих ключей: у
 * пользователя без свежих серий набор пуст, и первая же вышедшая серия иначе приняла бы себя за
 * «первый прогон» и осталась без пуша.
 */
const val LIBRARY_EPISODE_BASELINE_KEY = "baseline"
