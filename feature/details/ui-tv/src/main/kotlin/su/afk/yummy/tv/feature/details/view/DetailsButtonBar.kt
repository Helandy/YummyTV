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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.VideosUiState

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
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: Map<String, WatchProgressEntry>,
    canSubscribe: Boolean,
    hasCollections: Boolean,
    restoreFocusRequest: Int,
    firstFocusRequester: FocusRequester,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSubscriptionsSelected: () -> Unit,
    onDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val resumeEntry = watchProgress.values
        .filter { it.animeId == details.id && it.positionMs > 0 }
        .maxByOrNull { it.updatedAt }
    val watchLabel = when {
        isWatchLoading -> stringResource(R.string.details_loading_episodes)
        videosState is VideosUiState.Empty -> stringResource(R.string.details_watch_not_found)
        resumeEntry != null && resumeEntry.episode.isNotBlank() ->
            stringResource(R.string.details_continue_episode, resumeEntry.episode)
        else -> stringResource(R.string.details_watch)
    }

    val buttons = listOf(
        ButtonData(watchLabel, ButtonStyle.Filled, onWatchSelected),
        ButtonData(
            label = if (isInLibrary) {
                (libraryList ?: UserAnimeList.WATCHING).label()
            } else {
                stringResource(R.string.details_add_library)
            },
            style = if (isInLibrary) ButtonStyle.Outlined else ButtonStyle.Normal,
            onClick = onLibraryToggle,
        ),
        ButtonData(
            label = stringResource(
                if (isFavorite) R.string.details_remove_favorite
                else R.string.details_add_favorite,
            ),
            style = if (isFavorite) ButtonStyle.Outlined else ButtonStyle.Normal,
            onClick = onFavoriteToggle,
        ),
    ) + if (canSubscribe) {
        listOf(
            ButtonData(
                label = stringResource(R.string.details_subscriptions),
                style = ButtonStyle.Normal,
                onClick = onSubscriptionsSelected,
            )
        )
    } else {
        emptyList()
    } + listOf(
        ButtonData(stringResource(R.string.details_full_details), ButtonStyle.Normal, onDetailsSelected),
        ButtonData(stringResource(R.string.details_episodes), ButtonStyle.Normal, onEpisodesSelected),
        ButtonData(stringResource(R.string.details_trailers), ButtonStyle.Normal, onTrailersSelected),
        ButtonData(stringResource(R.string.details_similar), ButtonStyle.Normal, onSimilarSelected),
        ButtonData(stringResource(R.string.details_viewing_order), ButtonStyle.Normal, onViewingOrderSelected),
        ButtonData(stringResource(R.string.details_rating_button), ButtonStyle.Normal, onRatingSelected),
    ) + if (hasCollections) {
        listOf(ButtonData(stringResource(R.string.details_collections_button), ButtonStyle.Normal, onCollectionsSelected))
    } else {
        emptyList()
    } + if (details.screenshots.isNotEmpty()) {
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

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            withFrameNanos { }
            requestFocusedButton()
        }
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
            .height(height),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        .height(40.dp)
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
private fun UserAnimeList.label(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.details_library_list_watching
        UserAnimeList.PLANNED -> R.string.details_library_list_planned
        UserAnimeList.COMPLETED -> R.string.details_library_list_completed
        UserAnimeList.POSTPONED -> R.string.details_library_list_postponed
        UserAnimeList.DROPPED -> R.string.details_library_list_dropped
    }
)

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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
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
