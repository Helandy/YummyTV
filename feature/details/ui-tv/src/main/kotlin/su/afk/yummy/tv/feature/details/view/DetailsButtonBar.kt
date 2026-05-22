package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.R

private enum class ButtonStyle { Filled, Outlined, Normal }

private data class ButtonData(
    val label: String,
    val style: ButtonStyle,
    val onClick: () -> Unit,
)

@Composable
internal fun DetailsButtonBar(
    details: AnimeDetails,
    isInLibrary: Boolean,
    watchProgress: Map<String, WatchProgressEntry>,
    firstFocusRequester: FocusRequester,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeEntry = watchProgress.values
        .filter { it.animeId == details.id && it.positionMs > 0 }
        .maxByOrNull { it.updatedAt }
    val watchLabel = if (resumeEntry != null && resumeEntry.episode.isNotBlank()) {
        stringResource(R.string.details_continue_episode, resumeEntry.episode)
    } else {
        stringResource(R.string.details_watch)
    }

    val buttons = listOf(
        ButtonData(watchLabel, ButtonStyle.Filled, onWatchSelected),
        ButtonData(
            label = if (isInLibrary) {
                stringResource(R.string.details_in_library)
            } else {
                stringResource(R.string.details_add_library)
            },
            style = if (isInLibrary) ButtonStyle.Outlined else ButtonStyle.Normal,
            onClick = onLibraryToggle,
        ),
        ButtonData(stringResource(R.string.details_episodes), ButtonStyle.Normal, onEpisodesSelected),
        ButtonData(stringResource(R.string.details_trailers), ButtonStyle.Normal, onTrailersSelected),
        ButtonData(stringResource(R.string.details_similar), ButtonStyle.Normal, onSimilarSelected),
        ButtonData(stringResource(R.string.details_viewing_order), ButtonStyle.Normal, onViewingOrderSelected),
    ) + if (details.screenshots.isNotEmpty()) {
        listOf(ButtonData(stringResource(R.string.details_screenshots_title), ButtonStyle.Normal, onScreenshotsSelected))
    } else {
        emptyList()
    }
    val initialFocusedIndex = 0
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var focusedIndex by rememberSaveable { mutableIntStateOf(initialFocusedIndex) }
    val focusRequesters = remember(buttons.size) {
        List(buttons.size) { index ->
            if (index == initialFocusedIndex) firstFocusRequester else FocusRequester()
        }
    }
    val requestFocusedButton: () -> Unit = {
        val targetIndex = focusedIndex.coerceIn(0, buttons.lastIndex)
        scope.launch {
            listState.scrollToItem((targetIndex - 1).coerceAtLeast(0))
            focusRequesters.getOrNull(targetIndex)?.requestFocus()
        }
    }

    LaunchedEffect(buttons.size) {
        requestFocusedButton()
    }

    DisposableEffect(lifecycleOwner, buttons.size, focusedIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestFocusedButton()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = buttons,
                key = { index, _ -> index },
            ) { index, button ->
                val isFocused = focusedIndex == index
                val itemAlpha = when {
                    isFocused -> 1f
                    kotlin.math.abs(focusedIndex - index) == 1 -> 0.54f
                    else -> 0.24f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 2.dp),
                ) {
                    ActionButton(
                        label = button.label,
                        style = button.style,
                        alpha = itemAlpha,
                        onClick = button.onClick,
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedIndex = index
                                    scope.launch { listState.animateScrollToItem((index - 1).coerceAtLeast(0)) }
                                }
                            },
                    )
                }
            }
        }

        if (focusedIndex > 0) {
            ButtonFadeOverlay(alignment = Alignment.TopCenter)
        }
        if (focusedIndex < buttons.lastIndex) {
            ButtonFadeOverlay(alignment = Alignment.BottomCenter)
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    style: ButtonStyle,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val bgColor = when (style) {
        ButtonStyle.Filled -> MaterialTheme.colorScheme.onSurface
        ButtonStyle.Outlined -> MaterialTheme.colorScheme.primary
        ButtonStyle.Normal -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    }
    val textColor = when (style) {
        ButtonStyle.Filled -> MaterialTheme.colorScheme.surface
        ButtonStyle.Outlined -> MaterialTheme.colorScheme.onPrimary
        ButtonStyle.Normal -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(bgColor.copy(alpha = bgColor.alpha * alpha), shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = textColor.alpha * alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BoxScope.ButtonFadeOverlay(alignment: Alignment) {
    val surface = MaterialTheme.colorScheme.surface
    val brush = when (alignment) {
        Alignment.TopCenter -> Brush.verticalGradient(
            colors = listOf(surface.copy(alpha = 0.88f), surface.copy(alpha = 0f)),
        )
        else -> Brush.verticalGradient(
            colors = listOf(surface.copy(alpha = 0f), surface.copy(alpha = 0.88f)),
        )
    }
    Box(
        modifier = Modifier
            .align(alignment)
            .fillMaxWidth()
            .height(42.dp)
            .background(brush),
    )
}
