package su.afk.yummy.tv.feature.home.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.formatting.formatAirDate
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.presentation.R

internal fun HomeFeedSectionType.showMobileCardMetadata(): Boolean = when (this) {
    HomeFeedSectionType.SCHEDULE,
    HomeFeedSectionType.MY_NEW_EPISODES,
    HomeFeedSectionType.NEW_RELEASES,
    HomeFeedSectionType.RECOMMENDATIONS,
    HomeFeedSectionType.COLLECTIONS -> false
}

@Composable
internal fun HomeContinueWatchingItem.episodeSubtitle(): String =
    if (episode.isBlank()) {
        stringResource(R.string.home_episode)
    } else {
        stringResource(R.string.home_episode_number, episode)
    }

internal fun HomeContinueWatchingItem.timingSubtitle(): String =
    if (durationMs > 0L) {
        "${positionMs.millisToClockTime()} / ${durationMs.millisToClockTime()}"
    } else {
        positionMs.millisToClockTime()
    }

internal fun HomeContinueWatchingItem.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)


/** Дата выхода серии под названием карточки; без неё подписи нет. */
internal fun HomeFeedItem.newEpisodeSubtitle(): String? = airedAtSeconds?.formatAirDate()

/** Бейдж с номером вышедшей серии поверх постера: с галочкой, если серия уже просмотрена. */
@Composable
internal fun HomeFeedItem.newEpisodeBadge(): String? =
    episodeNumber?.let { number ->
        val label = if (isWatched) {
            R.string.home_new_episode_badge_watched
        } else {
            R.string.home_new_episode_badge
        }
        stringResource(label, number)
    }
