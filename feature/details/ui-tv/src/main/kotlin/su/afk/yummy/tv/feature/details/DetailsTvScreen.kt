package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.view.BalancerPickerOverlay
import su.afk.yummy.tv.feature.details.view.DetailsContent
import su.afk.yummy.tv.feature.details.view.DetailsError

@Composable
private fun BalancerOptionItem(
    label: String,
    focusRequester: FocusRequester?,
    isSupported: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    if (isSupported) {
        val bgColor by animateColorAsState(
            targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "balancer_bg",
        )
        val textColor by animateColorAsState(
            targetValue = if (focused) Color.Black else Color.White,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "balancer_text",
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .clip(shape)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
    } else {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.35f),
            )
            Text(
                text = stringResource(R.string.details_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
fun DetailsTvScreen(
    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,
) {
    val error = state.error
    val details = state.details
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && details == null -> TvLoadingScreen()
            error != null && details == null -> DetailsError(
                message = error,
                onRetry = { onEvent(DetailsState.Event.RetrySelected) },
            )
            details != null -> DetailsContent(
                details = details,
                videosState = state.videosState,
                watchProgress = state.watchProgress,
                isInLibrary = state.isInLibrary,
                onWatchSelected = {
                    val videos = (state.videosState as? VideosUiState.Content)?.videos.orEmpty()
                    val resumeEntry = state.watchProgress.values
                        .filter { it.animeId == details.id && it.positionMs > 0 }
                        .maxByOrNull { it.updatedAt }
                    val resumeVideo = resumeEntry?.let { entry ->
                        videos.firstOrNull { it.iframeUrl == entry.episodeUrl }
                    }
                    val pick = resumeVideo ?: run {
                        val kodikVideos = videos.filter { it.player.contains("kodik", ignoreCase = true) }
                        val source = kodikVideos.ifEmpty { videos }
                        source.groupBy { it.dubbing }
                            .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
                            ?.value
                            ?.minByOrNull { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
                            ?: source.firstOrNull()
                    }
                    if (pick != null) onEvent(DetailsState.Event.VideoSelected(pick))
                },
                onLibraryToggle = { onEvent(DetailsState.Event.LibraryToggled) },
                onEpisodesSelected = { onEvent(DetailsState.Event.EpisodesSelected) },
                onTrailersSelected = { onEvent(DetailsState.Event.TrailersSelected) },
                onSimilarSelected = { onEvent(DetailsState.Event.SimilarSelected) },
                onViewingOrderSelected = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                onScreenshotsSelected = { onEvent(DetailsState.Event.ScreenshotsSelected) },
            )
            else -> TvLoadingScreen()
        }

        if (state.showPosterFullscreen && details != null) {
            val closeFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = details.poster?.run { fullsize ?: big ?: medium ?: small },
                    contentDescription = details.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .focusRequester(closeFocusRequester)
                        .tvFocusableClick(
                            onClick = { onEvent(DetailsState.Event.PosterDismissed) },
                            shape = CircleShape,
                        )
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.details_close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        val balancerPicker = state.pendingBalancerSelection
        BackHandler(enabled = balancerPicker != null) {
            onEvent(DetailsState.Event.BalancerPickerDismissed)
        }
        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option -> onEvent(DetailsState.Event.BalancerConfirmed(option.video)) },
                onDismiss = { onEvent(DetailsState.Event.BalancerPickerDismissed) },
            )
        }
    }
}
