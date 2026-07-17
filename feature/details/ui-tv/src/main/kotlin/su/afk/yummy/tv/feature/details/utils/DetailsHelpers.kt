package su.afk.yummy.tv.feature.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl

internal fun Double.formatRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    return rounded.toString()
}

@Composable
internal fun Int.formatViews(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.details_views_millions,
        "%.1f".format(this / 1_000_000f)
    )

    this >= 1_000 -> stringResource(R.string.details_views_thousands, this / 1_000)
    else -> toString()
}

@Composable
internal fun AnimeEpisodes.formatAiredProgress(status: String? = null): String? {
    if (status.isReleasedAnimeStatus()) {
        val episodesCount = count ?: aired ?: return null
        return stringResource(R.string.details_released_episodes, episodesCount)
    }
    val airedCount = aired ?: return null
    val totalCount = count?.toString() ?: stringResource(R.string.details_unknown_count)
    val progress = stringResource(
        R.string.details_aired,
        stringResource(R.string.details_aired_progress, airedCount, totalCount)
    )
    val releaseCountdown = formatReleaseCountdown() ?: return progress
    return stringResource(R.string.details_aired_with_release, progress, releaseCountdown)
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
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.details_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.details_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.details_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}

internal fun Int.formatDuration(): String {
    val m = this / 60
    val s = this % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

internal val AnimePoster.bestUrl: String?
    get() = big ?: medium ?: fullsize ?: small


internal fun AnimeVideo.isAlloha(): Boolean =
    player.isAllohaPlayerUrl() || iframeUrl.isAllohaPlayerUrl()
