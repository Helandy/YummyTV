package su.afk.yummy.tv.feature.details.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.utils.EpisodeReleaseCountdown
import su.afk.yummy.tv.feature.details.utils.releaseCountdown
import java.util.Locale

internal fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal fun Double.formatRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    return String.format(Locale.US, "%.1f", rounded)
}

internal fun Int.formatViews(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000f)
    this >= 1_000 -> "${this / 1_000}K"
    else -> toString()
}

@Composable
internal fun AnimeEpisodes.formatAiredProgress(): String? {
    val airedCount = aired ?: return null
    val totalCount = count?.toString() ?: stringResource(R.string.details_mobile_unknown_count)
    val progress = stringResource(R.string.details_mobile_aired, airedCount, totalCount)
    val releaseCountdown = formatReleaseCountdown() ?: return progress
    return stringResource(R.string.details_mobile_aired_with_release, progress, releaseCountdown)
}

@Composable
private fun AnimeEpisodes.formatReleaseCountdown(): String? {
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
    val countdown = releaseCountdown(nowEpochSeconds) ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.details_mobile_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.details_mobile_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.details_mobile_release_in_minutes
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
            stringResource(R.string.details_mobile_loading_episodes)
        }
        videosState is VideosUiState.Empty -> stringResource(R.string.details_mobile_watch_not_found)
        continueTarget != null && continueTarget.video.episode.isNotBlank() -> {
            stringResource(R.string.details_mobile_continue_episode, continueTarget.video.episode)
        }
        else -> stringResource(R.string.details_mobile_watch)
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
        UserAnimeList.WATCHING -> R.string.details_mobile_library_watching
        UserAnimeList.PLANNED -> R.string.details_mobile_library_planned
        UserAnimeList.COMPLETED -> R.string.details_mobile_library_completed
        UserAnimeList.POSTPONED -> R.string.details_mobile_library_postponed
        UserAnimeList.DROPPED -> R.string.details_mobile_library_dropped
    }
)
