package su.afk.yummy.tv.feature.details.episodes.model

/** Фильтр списка серий по статусу просмотра. */
enum class EpisodeWatchFilter {
    /** Все серии. */
    All,

    /** Серии, которые не досмотрены до конца (не начатые и начатые). */
    Unwatched,

    /** Досмотренные серии. */
    Watched,
}
