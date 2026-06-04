package su.afk.yummy.tv.feature.details.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.anime.model.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.R

internal fun Double.formatRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    return rounded.toString()
}

@Composable
internal fun Int.formatViews(): String = when {
    this >= 1_000_000 -> stringResource(R.string.details_views_millions, "%.1f".format(this / 1_000_000f))
    this >= 1_000 -> stringResource(R.string.details_views_thousands, this / 1_000)
    else -> toString()
}

@Composable
internal fun AnimeEpisodes.formatAiredProgress(): String? {
    val airedCount = aired ?: return null
    val totalCount = count?.toString() ?: stringResource(R.string.details_unknown_count)
    return stringResource(R.string.details_aired, stringResource(R.string.details_aired_progress, airedCount, totalCount))
}

internal fun Int.formatDuration(): String {
    val m = this / 60
    val s = this % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

internal val AnimePoster.bestUrl: String?
    get() = big ?: medium ?: fullsize ?: small


internal fun AnimeVideo.isAlloha(): Boolean =
    player.contains("alloha", ignoreCase = true) ||
    iframeUrl.contains("alloha", ignoreCase = true)
