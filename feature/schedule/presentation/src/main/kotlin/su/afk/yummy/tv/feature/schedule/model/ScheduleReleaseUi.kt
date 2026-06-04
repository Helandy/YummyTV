package su.afk.yummy.tv.feature.schedule.model

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem

data class ScheduleReleaseUi(
    val item: AnimeScheduleItem,
    val epochSeconds: Long,
    val episode: Int,
    val aired: Boolean,
) {
    val focusKey: String = "${item.animeId}:$epochSeconds:$aired"
}
