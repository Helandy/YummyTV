package su.afk.yummy.tv.feature.home.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.tv.TvProgressMediaCard
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailModels
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailOf
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.feature.home.presentation.R

private val CardWidth = 220.dp
private val ThumbnailHeight = 124.dp

@Composable
internal fun ContinueWatchingCard(
    entry: HomeContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onFocused: () -> Unit = {},
    leftFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    val progress = if (entry.durationMs > 0) {
        (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val positionLabel = entry.positionMs.millisToClockTime()
    val durationLabel = entry.durationMs.millisToClockTime()
    val timingLabel =
        if (entry.durationMs > 0L) "$positionLabel / $durationLabel" else positionLabel

    val imageModels = episodeThumbnailModels(
        thumbnail = episodeThumbnailOf(entry.animeId, entry.episode, entry.screenshotUrl, entry.episodeUrl),
        posterUrls = entry.poster.urlsByQuality(LocalPosterQuality.current),
    )
    val episodeLabel = if (entry.episode.isNotBlank()) {
        stringResource(R.string.home_episode_number, entry.episode)
    } else {
        stringResource(R.string.home_episode)
    }

    TvProgressMediaCard(
        title = entry.animeTitle,
        imageModels = imageModels,
        subtitle = episodeLabel,
        trailingSubtitle = timingLabel,
        progress = progress,
        onClick = onClick,
        modifier = modifier,
        width = CardWidth,
        thumbnailHeight = ThumbnailHeight,
        leftFocusRequester = leftFocusRequester,
        upFocusRequester = upFocusRequester,
        downFocusRequester = downFocusRequester,
        onFocused = onFocused,
        isLoading = isLoading,
    )
}
