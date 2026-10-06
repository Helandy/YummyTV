package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.home.utils.lastAiredSeconds
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import javax.inject.Inject

/**
 * Возвращает тайтлы, у которых последняя серия вышла не раньше [RECENT_WINDOW_SECONDS] назад,
 * от свежих к старым. Берёт кэшированное расписание (один запрос на всех), а не опрашивает тайтлы.
 * Анонсы без вышедших серий сюда не попадают: у них нет даты последней серии.
 */
class GetRecentlyAiredScheduleUseCase @Inject constructor(
    private val getSchedule: GetAnimeScheduleUseCase,
) {
    suspend operator fun invoke(nowSeconds: Long): List<HomeFeedItem> {
        val threshold = nowSeconds - RECENT_WINDOW_SECONDS
        return getSchedule()
            .flatMap { it.items }
            .mapNotNull { item ->
                val lastAired = item.lastAiredSeconds(nowSeconds) ?: return@mapNotNull null
                item.takeIf { lastAired >= threshold }?.let { lastAired to it }
            }
            .sortedByDescending { (lastAired, _) -> lastAired }
            .distinctBy { (_, item) -> item.animeId }
            .map { (lastAired, item) ->
                HomeFeedItem(
                    id = item.animeId,
                    title = item.title,
                    description = "",
                    poster = item.posterUrl?.let { HomePoster(null, null, null, null, mega = it) },
                    rating = null,
                    year = null,
                    action = HomeFeedItemAction.OpenSeries(item.animeId),
                    episodeNumber = item.airedEpisodes,
                    airedAtSeconds = lastAired,
                )
            }
    }

    private companion object {
        const val RECENT_WINDOW_SECONDS = 14L * 24 * 60 * 60
    }
}
