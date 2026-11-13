package su.afk.yummy.tv.feature.home.model

import su.afk.yummy.tv.domain.home.model.HomeFeedItem

/** Данные, из которых к исходной ленте добавляется секция «Мои новые серии». */
internal data class HomeFeedVisibilityParams(
    /** Секция выключена в настройках — не собирается вовсе. */
    val newEpisodesEnabled: Boolean = true,
    /** Тайтлы из выбранных в настройках списков. */
    val libraryAnimeIds: Set<Int> = emptySet(),
    /** Онгоинги с серией за последние дни из расписания. */
    val recentlyAired: List<HomeFeedItem> = emptyList(),
    /** Просмотренные серии: id тайтла → номера серий. */
    val watchedEpisodes: Map<Int, Set<Int>> = emptyMap(),
)
