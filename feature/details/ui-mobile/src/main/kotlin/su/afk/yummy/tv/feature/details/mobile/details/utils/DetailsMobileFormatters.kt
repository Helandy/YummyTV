package su.afk.yummy.tv.feature.details.mobile.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.core.common.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.core.common.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.common.episode.releaseCountdown
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.formatReleaseWeekdayTime
import su.afk.yummy.tv.feature.details.utils.nextEpisodeNumber
import su.afk.yummy.tv.feature.details.model.DetailsWatchRestart
import su.afk.yummy.tv.feature.details.utils.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.utils.resolveDetailsWatchRestart
import java.util.Locale

internal fun Int.formatViews(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000f)
    this >= 1_000 -> "${this / 1_000}K"
    else -> toString()
}

@Composable
internal fun AnimeEpisodes.formatAiredProgress(status: String?): String? {
    if (status.isReleasedAnimeStatus()) {
        val episodesCount = count ?: aired ?: return null
        return stringResource(R.string.details_released_episodes, episodesCount)
    }
    val airedCount = aired ?: return formatReleaseCountdown()
    val totalCount = count?.toString() ?: stringResource(R.string.details_unknown_count)
    val progress = stringResource(R.string.details_mobile_aired, airedCount, totalCount)
    val releaseCountdown = formatReleaseCountdown() ?: return progress
    return stringResource(R.string.details_aired_with_release, progress, releaseCountdown)
}

/** Отсчёт до [AnimeEpisodes.nextDateEpochSeconds], пересчитывается раз в минуту. */
@Composable
internal fun AnimeEpisodes.rememberReleaseCountdown(): EpisodeReleaseCountdown? {
    if (nextDateEpochSeconds == null) return null
    val nowEpochSeconds = rememberNowEpochSeconds()
    return releaseCountdown(nextDateEpochSeconds, nowEpochSeconds)
}

/** «Серия 8 — пт, 18:00»; `null`, если даты нет или серия уже вышла. */
@Composable
internal fun AnimeEpisodes.formatNextEpisodeRelease(): String? {
    val nextDate = nextDateEpochSeconds ?: return null
    rememberReleaseCountdown() ?: return null
    val dateTime = nextDate.formatReleaseWeekdayTime(rememberNowEpochSeconds())
    return nextEpisodeNumber()
        ?.let { stringResource(R.string.details_next_episode_release, it, dateTime) }
        ?: stringResource(R.string.details_next_episode_release_unnumbered, dateTime)
}

@Composable
private fun AnimeEpisodes.formatReleaseCountdown(): String? {
    val countdown = rememberReleaseCountdown() ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.details_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.details_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.details_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}

@Composable
internal fun DetailsState.State.watchLabel(details: AnimeDetails): String {
    val continueTarget = (videosState as? VideosUiState.Content)?.let { content ->
        resolveDetailsContinueTarget(
            animeId = details.id,
            videos = content.videos,
            watchProgress = watchProgress,
        )
    }
    val watchRestart = (videosState as? VideosUiState.Content)?.let { content ->
        resolveDetailsWatchRestart(
            animeId = details.id,
            videos = content.videos,
            watchProgress = watchProgress,
            status = details.status,
        )
    }
    return when {
        isWatchLaunchPending || isWatchDataLoading -> {
            stringResource(R.string.details_loading_episodes)
        }

        videosState is VideosUiState.Empty -> stringResource(R.string.details_watch_not_found)
        watchRestart == DetailsWatchRestart.Restart -> stringResource(R.string.details_watch_restart)
        watchRestart == DetailsWatchRestart.Again -> stringResource(R.string.details_watch_again)
        continueTarget != null && continueTarget.video.episode.isNotBlank() -> {
            stringResource(R.string.details_continue_episode, continueTarget.video.episode)
        }

        else -> stringResource(R.string.details_watch)
    }
}

@Composable
internal fun DetailsState.State.libraryLabel(): String = when {
    isInLibrary -> (libraryList ?: UserAnimeList.WATCHING).label()
    else -> stringResource(R.string.details_mobile_add_library)
}

@Composable
internal fun UserAnimeList.label(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.details_library_list_watching
        UserAnimeList.PLANNED -> R.string.details_library_list_planned
        UserAnimeList.COMPLETED -> R.string.details_library_list_completed
        UserAnimeList.POSTPONED -> R.string.details_library_list_postponed
        UserAnimeList.DROPPED -> R.string.details_library_list_dropped
    },
)
