package su.afk.yummy.tv.feature.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.core.common.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.core.common.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.common.episode.releaseCountdown
import su.afk.yummy.tv.core.common.player.isAllohaPlayerUrl
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun Int.formatViews(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.details_count_millions,
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
    val airedCount = aired ?: return formatReleaseCountdown()
    val totalCount = count?.toString() ?: stringResource(R.string.details_unknown_count)
    val progress = stringResource(
        R.string.details_aired,
        stringResource(R.string.details_aired_progress, airedCount, totalCount)
    )
    val releaseCountdown = formatReleaseCountdown() ?: return progress
    return stringResource(R.string.details_aired_with_release, progress, releaseCountdown)
}

/** «Серия 8 — пт, 18:00»; `null`, если даты нет или серия уже вышла. */
@Composable
internal fun AnimeEpisodes.formatNextEpisodeRelease(): String? {
    val nextDate = nextDateEpochSeconds ?: return null
    val nowEpochSeconds = rememberNowEpochSeconds()
    if (nextDate <= nowEpochSeconds) return null
    val dateTime = nextDate.formatReleaseWeekdayTime(nowEpochSeconds)
    return nextEpisodeNumber()
        ?.let { stringResource(R.string.details_next_episode_release, it, dateTime) }
        ?: stringResource(R.string.details_next_episode_release_unnumbered, dateTime)
}

@Composable
private fun AnimeEpisodes.formatReleaseCountdown(): String? {
    if (nextDateEpochSeconds == null) return null
    val nowEpochSeconds = rememberNowEpochSeconds()
    val countdown = releaseCountdown(nextDateEpochSeconds, nowEpochSeconds) ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.details_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.details_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.details_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}

internal val AnimePoster.bestUrl: String?
    get() = big ?: medium ?: fullsize ?: small


internal fun AnimeVideo.isAlloha(): Boolean =
    player.isAllohaPlayerUrl() || iframeUrl.isAllohaPlayerUrl()
