package su.afk.yummy.tv.feature.details.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.anime.AnimePoster
import su.afk.yummy.tv.domain.anime.AnimeVideo
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
