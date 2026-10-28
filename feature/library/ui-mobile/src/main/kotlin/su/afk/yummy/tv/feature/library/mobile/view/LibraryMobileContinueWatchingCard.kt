package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileProgressMediaCard
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailModels
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailOf
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.timingLabel
import su.afk.yummy.tv.feature.library.mobile.utils.watchProgress

@Composable
internal fun LibraryMobileContinueWatchingCard(
    entry: HomeContinueWatchingItem,
    episodeLabel: String,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val imageModels = episodeThumbnailModels(
        thumbnail = episodeThumbnailOf(entry.animeId, entry.episode, entry.screenshotUrl, entry.episodeUrl),
        posterUrls = entry.poster.urlsByQuality(LocalPosterQuality.current),
    )

    MobileProgressMediaCard(
        title = entry.animeTitle.ifBlank { episodeLabel },
        imageModels = imageModels,
        subtitle = episodeLabel,
        trailingSubtitle = entry.timingLabel(),
        progress = entry.watchProgress(),
        modifier = modifier,
        isLoading = isLoading,
        placeholderText = entry.episode,
        imageOverlay = {
            ContinueWatchingOverlayButton(
                contentDescription = stringResource(
                    R.string.library_mobile_remove_content_description
                ),
                onClick = onDelete,
                isError = true,
                size = 36.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
            ContinueWatchingOverlayButton(
                contentDescription = stringResource(
                    R.string.library_mobile_details_content_description
                ),
                onClick = onDetails,
                size = 36.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
internal fun ContinueWatchingOverlayButton(
    contentDescription: String,
    onClick: () -> Unit,
    isError: Boolean = false,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    // Кнопка удаления — просто красный крестик без плашки, чтобы не кричала поверх кадра.
    val containerColor = if (isError) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
