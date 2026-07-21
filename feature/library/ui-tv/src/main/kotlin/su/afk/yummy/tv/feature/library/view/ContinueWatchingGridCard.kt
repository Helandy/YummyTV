package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.TvProgressMediaCard
import su.afk.yummy.tv.core.utils.KodikThumbnail
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImageModel
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.utils.bestUrl
import su.afk.yummy.tv.feature.library.utils.timingLabel

@Composable
internal fun ContinueWatchingGridCard(
    entry: HomeContinueWatchingItem,
    episodeLabel: String,
    cardWidth: Dp,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    detailsModifier: Modifier = Modifier,
    deleteModifier: Modifier = Modifier,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val imageModel = resolveContinueWatchingImageModel(
        screenshotUrl = entry.screenshotUrl,
        episodeUrl = entry.episodeUrl,
        posterUrl = entry.poster.bestUrl(),
        kodikThumbnailModel = ::KodikThumbnail,
    )
    val progress =
        if (entry.durationMs > 0L) {
            (entry.positionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .onFocusChanged { hasFocus = it.hasFocus }
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TvProgressMediaCard(
            title = entry.animeTitle.ifBlank { episodeLabel },
            imageModel = imageModel,
            subtitle = episodeLabel,
            trailingSubtitle = entry.timingLabel(),
            progress = progress,
            onClick = onClick,
            modifier = cardModifier,
            width = cardWidth,
            thumbnailHeight = cardWidth * 9f / 16f,
            leftFocusRequester = leftFocusRequester,
            rightFocusRequester = rightFocusRequester,
            upFocusRequester = upFocusRequester,
            downFocusRequester = downFocusRequester,
            onFocused = onFocused,
            focusedScale = 1f,
        )
        Row(
            modifier = Modifier
                .width(cardWidth)
                .alpha(if (hasFocus) 1f else 0f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LibraryDetailsButton(
                onClick = onDetails,
                modifier = detailsModifier.weight(1f),
            )
            LibraryDeleteButton(
                onClick = onDelete,
                modifier = deleteModifier.weight(1f),
            )
        }
    }
}
