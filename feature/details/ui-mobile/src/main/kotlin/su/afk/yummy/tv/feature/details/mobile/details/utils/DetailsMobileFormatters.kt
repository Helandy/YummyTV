package su.afk.yummy.tv.feature.details.mobile.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.utils.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.utils.episode.releaseCountdown
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.core.utils.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.resolveDetailsContinueTarget
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
    val nowEpochSeconds by produceState(
        initialValue = System.currentTimeMillis() / 1_000L,
        key1 = nextDateEpochSeconds,
    ) {
        while (true) {
            value = System.currentTimeMillis() / 1_000L
            delay(60_000L)
        }
    }
    return releaseCountdown(nextDateEpochSeconds, nowEpochSeconds)
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
    return when {
        isWatchLaunchPending || videosState is VideosUiState.Loading -> {
            stringResource(R.string.details_loading_episodes)
        }

        videosState is VideosUiState.Empty -> stringResource(R.string.details_watch_not_found)
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
