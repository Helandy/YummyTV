package su.afk.yummy.tv.feature.details.episodes.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeGroup
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchStatus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

/** Оставляет серии, подходящие под [filter] по статусу просмотра. */
fun filterEpisodeGroups(
    groups: ImmutableList<EpisodeGroup>,
    watchProgress: DetailsWatchProgressIndex,
    filter: EpisodeWatchFilter,
): ImmutableList<EpisodeGroup> {
    if (filter == EpisodeWatchFilter.All) return groups
    return groups.filter { group ->
        val watched = group.videos.watchStatus(watchProgress) is EpisodeWatchStatus.Watched
        if (filter == EpisodeWatchFilter.Watched) watched else !watched
    }.toImmutableList()
}
