package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import java.util.concurrent.TimeUnit

/** Убирает из блока рекомендаций тайтлы, скрытые пользователем. */
internal fun HomeFeed.withoutHiddenRecommendations(hiddenIds: Set<Int>): HomeFeed {
    if (hiddenIds.isEmpty()) return this
    return copy(
        sections = sections.map { section ->
            if (section.type == HomeFeedSectionType.RECOMMENDATIONS) {
                section.copy(items = section.items.filterNot { it.id in hiddenIds })
            } else {
                section
            }
        }
    )
}

/** Расписание вынесено в отдельную вкладку бокового меню, поэтому его секция не показывается в ленте главной. */
internal fun HomeFeed.withoutScheduleSection(): HomeFeed =
    copy(sections = sections.filterNot { it.type == HomeFeedSectionType.SCHEDULE })

internal fun HomeContinueWatchingItem.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

internal fun Long.toToastTimeString(): String {
    val totalSeconds = coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private val SUPPORT_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(7)

/** Сколько ещё ждать до показа окна поддержки: неделя с момента, когда оно стало доступно. */
internal fun SupportPromptSnapshot.supportPromptRemainingMs(nowMs: Long): Long =
    SUPPORT_PROMPT_DELAY_MS - (nowMs - firstEligibleTimeMs)
