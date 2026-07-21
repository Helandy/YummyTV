package su.afk.yummy.tv.feature.home.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.TvProgressMediaCard
import su.afk.yummy.tv.core.utils.KodikThumbnail
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImageModel
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.home.R
import su.afk.yummy.tv.feature.home.utils.bestUrl
import su.afk.yummy.tv.feature.home.utils.msToTimeString

private val CardWidth = 220.dp
private val ThumbnailHeight = 124.dp
private val InProgressColor = Color(0xFF4CAF50)

@Composable
internal fun ContinueWatchingCard(
    entry: HomeContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    val positionLabel = entry.positionMs.msToTimeString()
    val durationLabel = entry.durationMs.msToTimeString()
    val timingLabel =
        if (entry.durationMs > 0L) "$positionLabel / $durationLabel" else positionLabel

    val imageModel = resolveContinueWatchingImageModel(
        screenshotUrl = entry.screenshotUrl,
        episodeUrl = entry.episodeUrl,
        posterUrl = entry.poster.bestUrl(),
        kodikThumbnailModel = ::KodikThumbnail,
    )
    val episodeLabel = if (entry.episode.isNotBlank()) {
        stringResource(R.string.home_episode_number, entry.episode)
    } else {
        stringResource(R.string.home_episode)
    }

    TvProgressMediaCard(
        title = entry.animeTitle,
        imageModel = imageModel,
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
        progressColor = InProgressColor,
    )
}
