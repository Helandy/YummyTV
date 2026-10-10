package su.afk.yummy.tv.feature.home.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.formatting.formatAirDate
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.presentation.R

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
