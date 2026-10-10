package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileProgressMediaCard
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImageModel
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.mobile.utils.posterUrl
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
    val imageModel = resolveContinueWatchingImageModel(
        screenshotUrl = entry.screenshotUrl,
        episodeUrl = entry.episodeUrl,
        posterUrl = entry.poster.posterUrl(),
        kodikThumbnailModel = ::KodikThumbnail,
    )

    MobileProgressMediaCard(
        title = entry.animeTitle.ifBlank { episodeLabel },
        imageModel = imageModel,
        subtitle = episodeLabel,
        trailingSubtitle = entry.timingLabel(),
        progress = entry.watchProgress(),
        modifier = modifier,
        isLoading = isLoading,
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
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
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
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
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
