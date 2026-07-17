package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.model.ButtonData
import su.afk.yummy.tv.feature.details.details.model.ButtonRowData
import su.afk.yummy.tv.feature.details.details.model.ButtonStyle
import su.afk.yummy.tv.feature.details.details.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.details.utils.label

@Composable
internal fun DetailsButtonBar(
    details: AnimeDetails,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: DetailsWatchProgressIndex,
    canSubscribe: Boolean,
    buttonOrder: List<DetailsButtonAction> = SettingsStore.defaultDetailsButtonOrder,
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
    onReviewsSelected: () -> Unit,
    onBloggerVideosSelected: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val continueTarget = (videosState as? VideosUiState.Content)?.let { content ->
        resolveDetailsContinueTarget(
            animeId = details.id,
            videos = content.videos,
            watchProgress = watchProgress,
        )
    }
    val watchLabel = when {
        isWatchLoading -> stringResource(R.string.details_loading_episodes)
        videosState is VideosUiState.Empty -> stringResource(R.string.details_watch_not_found)
        continueTarget != null && continueTarget.video.episode.isNotBlank() ->
            stringResource(R.string.details_continue_episode, continueTarget.video.episode)

        else -> stringResource(R.string.details_watch)
    }

    val watchButton = ButtonData(
        action = DetailsButtonAction.WATCH,
        label = watchLabel,
        icon = Icons.Filled.PlayArrow,
        style = ButtonStyle.Filled,
        onClick = onWatchSelected,
    )
    val libraryButton = ButtonData(
        action = DetailsButtonAction.LIBRARY,
        label = if (isInLibrary) {
            (libraryList ?: UserAnimeList.WATCHING).label()
        } else {
            stringResource(R.string.details_add_library)
        },
        icon = if (isInLibrary) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
        style = if (isInLibrary) ButtonStyle.Outlined else ButtonStyle.Normal,
        onClick = onLibraryToggle,
    )
    val favoriteButton = ButtonData(
        action = DetailsButtonAction.FAVORITE,
        label = stringResource(
            if (isFavorite) R.string.details_remove_favorite
            else R.string.details_add_favorite,
        ),
        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        style = if (isFavorite) ButtonStyle.Outlined else ButtonStyle.Normal,
        onClick = onFavoriteToggle,
    )
    val secondaryButtons = listOf(
        ButtonData(
            DetailsButtonAction.EPISODES,
            stringResource(R.string.details_episodes),
            Icons.Filled.VideoLibrary,
            ButtonStyle.Normal,
            onEpisodesSelected,
        ),
    ) + if (canSubscribe) {
        listOf(
            ButtonData(
                action = DetailsButtonAction.SUBSCRIPTIONS,
                label = stringResource(R.string.details_subscriptions),
                icon = Icons.Filled.Notifications,
                style = ButtonStyle.Normal,
                onClick = onSubscriptionsSelected,
            )
        )
    } else {
        emptyList()
    } + listOf(
        ButtonData(
            DetailsButtonAction.FULL_DETAILS,
            stringResource(R.string.details_full_details),
            Icons.Filled.Info,
            ButtonStyle.Normal,
            onDetailsSelected,
        ),
        ButtonData(
            DetailsButtonAction.TRAILERS,
            stringResource(R.string.details_trailers),
            Icons.Filled.Movie,
            ButtonStyle.Normal,
            onTrailersSelected,
        ),
        ButtonData(
            DetailsButtonAction.SIMILAR,
            stringResource(R.string.details_similar),
            Icons.Filled.AutoAwesome,
            ButtonStyle.Normal,
            onSimilarSelected,
        ),
        ButtonData(
            DetailsButtonAction.VIEWING_ORDER,
            stringResource(R.string.details_viewing_order),
            Icons.Filled.FormatListNumbered,
            ButtonStyle.Normal,
            onViewingOrderSelected,
        ),
        ButtonData(
            DetailsButtonAction.RATING,
            stringResource(R.string.details_rating_button),
            Icons.Filled.Star,
            ButtonStyle.Normal,
            onRatingSelected,
        ),
        ButtonData(
            DetailsButtonAction.COLLECTIONS,
            stringResource(R.string.details_collections_button),
            Icons.Filled.CollectionsBookmark,
            ButtonStyle.Normal,
            onCollectionsSelected,
        ),
        ButtonData(
            DetailsButtonAction.REVIEWS,
            if (details.reviewsCount > 0) stringResource(
                R.string.details_reviews_count,
                details.reviewsCount
            ) else stringResource(R.string.details_reviews),
            Icons.Filled.RateReview,
            ButtonStyle.Normal,
            onReviewsSelected,
        ),
        ButtonData(
            DetailsButtonAction.BLOGGER_VIDEOS,
            stringResource(R.string.details_blogger_videos),
            Icons.Filled.VideoCameraFront,
            ButtonStyle.Normal,
            onBloggerVideosSelected,
        ),
    ) + if (details.screenshots.isNotEmpty()) {
        listOf(
            ButtonData(
                DetailsButtonAction.SCREENSHOTS,
                stringResource(R.string.details_screenshots_title),
                Icons.Filled.PhotoLibrary,
                ButtonStyle.Normal,
                onScreenshotsSelected,
            )
        )
    } else {
        emptyList()
    }
    val availableButtons = (listOf(watchButton, libraryButton, favoriteButton) + secondaryButtons)
        .associateBy { it.action }
    val buttons = buttonOrder.mapNotNull { availableButtons[it] } +
            availableButtons.values.filterNot { button -> button.action in buttonOrder }
    val buttonRows = buttons.toButtonRows()
    val initialFocusedAction = buttons.firstOrNull()?.action ?: DetailsButtonAction.WATCH
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var focusedAction by rememberSaveable { mutableStateOf(initialFocusedAction) }
    fun focusedButtonIndex(): Int =
        buttons.indexOfFirst { it.action == focusedAction }.takeIf { it >= 0 } ?: 0

    val currentFocusedIndex = focusedButtonIndex().coerceIn(0, buttons.lastIndex)
    val focusRequesters = remember(buttons.map { it.action }) {
        List(buttons.size) { index ->
            if (index == 0) firstFocusRequester else FocusRequester()
        }
    }
    // Отступ, позволяющий отцентрировать сфокусированную строку: сверху ровно на одну
    // строку меньше (первая строка прижата к верху), снизу — до центра последней.
    val verticalCenterInset = (height - ButtonRowHeight) / 2
    val topPadding = (verticalCenterInset - ButtonRowHeight - ButtonRowSpacing).coerceAtLeast(0.dp)
    val centerScrollOffset = with(LocalDensity.current) { -verticalCenterInset.roundToPx() }

    fun rowScrollOffset(rowIndex: Int): Int = if (rowIndex == 0) 0 else centerScrollOffset

    val requestFocusedButton: () -> Unit = {
        val targetIndex = focusedButtonIndex().coerceIn(0, buttons.lastIndex)
        scope.launch {
            val targetRow = buttonRows.rowIndexForButton(targetIndex)
            listState.scrollToItem(targetRow, rowScrollOffset(targetRow))
            focusRequesters.getOrNull(targetIndex)?.requestFocus()
        }
    }

    fun onRowFocused(rowIndex: Int) {
        scope.launch {
            listState.animateScrollToItem(rowIndex, rowScrollOffset(rowIndex))
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

    DisposableEffect(lifecycleOwner, buttons.size, focusedAction) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestFocusedButton()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        // i = W*(s-1)/(2s): при ширине кнопки W-2i и масштабе s выступ (W-2i)*(s-1)/2 == i,
        // так что сфокусированная кнопка встаёт ровно по краю бара.
        val focusInset = maxWidth * ((ButtonFocusedScale - 1f) / (2f * ButtonFocusedScale))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentPadding = PaddingValues(
                start = focusInset,
                end = focusInset,
                top = topPadding,
                bottom = verticalCenterInset,
            ),
            verticalArrangement = Arrangement.spacedBy(ButtonRowSpacing),
        ) {
            itemsIndexed(
                items = buttonRows,
                key = { _, row -> row.key },
            ) { rowIndex, row ->
                when (row) {
                    is ButtonRowData.Single -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        ) {
                            DetailsActionButton(
                                button = row.button,
                                index = row.index,
                                focusRequester = focusRequesters[row.index],
                                onFocused = {
                                    focusedAction = row.button.action
                                    onRowFocused(rowIndex)
                                },
                            )
                        }
                    }

                    is ButtonRowData.LibraryFavorite -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DetailsActionButton(
                                button = row.libraryButton,
                                index = row.libraryIndex,
                                focusRequester = focusRequesters[row.libraryIndex],
                                onFocused = {
                                    focusedAction = row.libraryButton.action
                                    onRowFocused(rowIndex)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            DetailsActionButton(
                                button = row.favoriteButton,
                                index = row.favoriteIndex,
                                focusRequester = focusRequesters[row.favoriteIndex],
                                onFocused = {
                                    focusedAction = row.favoriteButton.action
                                    onRowFocused(rowIndex)
                                },
                                showLabel = false,
                                iconSize = 24.dp,
                                verticalPadding = 5.dp,
                                focusedScale = 1.10f,
                                modifier = Modifier.width(56.dp),
                            )
                        }
                    }
                }
            }
        }

    }
}

private val ButtonRowHeight = 40.dp
private val ButtonRowSpacing = 6.dp
private const val ButtonFocusedScale = 1.04f

private fun List<ButtonData>.toButtonRows(): List<ButtonRowData> = buildList {
    var index = 0
    while (index <= this@toButtonRows.lastIndex) {
        val button = this@toButtonRows[index]
        val nextButton = this@toButtonRows.getOrNull(index + 1)
        if (button.action == DetailsButtonAction.LIBRARY && nextButton?.action == DetailsButtonAction.FAVORITE) {
            add(ButtonRowData.LibraryFavorite(index, button, index + 1, nextButton))
            index += 2
        } else {
            add(ButtonRowData.Single(index, button))
            index += 1
        }
    }
}

private fun List<ButtonRowData>.rowIndexForButton(buttonIndex: Int): Int =
    indexOfFirst { buttonIndex in it.buttonIndices }.coerceAtLeast(0)

@Composable
private fun DetailsActionButton(
    button: ButtonData,
    index: Int,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: Dp = 18.dp,
    verticalPadding: Dp = 8.dp,
    focusedScale: Float = ButtonFocusedScale,
) {
    ActionButton(
        label = button.label,
        icon = button.icon,
        style = button.style,
        showLabel = showLabel,
        iconSize = iconSize,
        verticalPadding = verticalPadding,
        focusedScale = focusedScale,
        onClick = {
            onFocused()
            button.onClick()
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            },
    )
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    style: ButtonStyle,
    showLabel: Boolean = true,
    iconSize: Dp = 18.dp,
    verticalPadding: Dp = 8.dp,
    focusedScale: Float = ButtonFocusedScale,
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
            .tvFocusableClick(onClick = onClick, shape = shape, focusedScale = focusedScale)
            .background(bgColor, shape)
            .padding(horizontal = 20.dp, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(iconSize),
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
