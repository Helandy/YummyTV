package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.utils.formatting.toCompactCount
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.ButtonData
import su.afk.yummy.tv.feature.details.details.model.ButtonStyle
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.details.utils.label
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.model.DetailsWatchRestart
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.utils.resolveDetailsWatchRestart
import su.afk.yummy.tv.feature.details.utils.statusColor
import su.afk.yummy.tv.feature.details.utils.statusIcon

@Composable
internal fun DetailsButtonBar(
    details: AnimeDetails,
    hasScreenshots: Boolean,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: DetailsWatchProgressIndex,
    canSubscribe: Boolean,
    restoreFocusRequest: Int,
    hiddenActions: Set<DetailsButtonAction>,
    focusOnResume: Boolean,
    firstFocusRequester: FocusRequester,
    bottomFocusRequester: FocusRequester?,
    /** Первая кнопка нижнего ряда — вход в бар снизу (из описания, из ленты). */
    lastRowEntryFocusRequester: FocusRequester,
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
    onCommentsSelected: () -> Unit,
    onReviewsSelected: () -> Unit,
    onBloggerVideosSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val continueTarget = (videosState as? VideosUiState.Content)?.let { content ->
        resolveDetailsContinueTarget(
            animeId = details.id,
            videos = content.videos,
            watchProgress = watchProgress,
        )
    }
    val watchRestart = (videosState as? VideosUiState.Content)?.let { content ->
        resolveDetailsWatchRestart(
            animeId = details.id,
            videos = content.videos,
            watchProgress = watchProgress,
            status = details.status,
        )
    }
    val watchLabel = when {
        isWatchLoading -> stringResource(R.string.details_loading_episodes)
        videosState is VideosUiState.Empty -> stringResource(R.string.details_watch_not_found)
        watchRestart == DetailsWatchRestart.Restart -> stringResource(R.string.details_watch_restart)
        watchRestart == DetailsWatchRestart.Again -> stringResource(R.string.details_watch_again)
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
        enabled = !isWatchLoading,
    )
    val libraryButton = ButtonData(
        action = DetailsButtonAction.LIBRARY,
        label = if (isInLibrary) {
            (libraryList ?: UserAnimeList.WATCHING).label()
        } else {
            stringResource(R.string.details_add_library)
        },
        icon = if (isInLibrary) {
            (libraryList ?: UserAnimeList.WATCHING).statusIcon()
        } else {
            Icons.AutoMirrored.Filled.PlaylistAdd
        },
        style = if (isInLibrary) ButtonStyle.Outlined else ButtonStyle.Normal,
        onClick = onLibraryToggle,
        overrideColor = if (isInLibrary) {
            (
                libraryList
                    ?: UserAnimeList.WATCHING
                ).statusColor()
        } else {
            null
        },
    )
    val favoriteButton = ButtonData(
        action = DetailsButtonAction.FAVORITE,
        label = stringResource(
            if (isFavorite) {
                R.string.details_add_favorite
            } else {
                R.string.details_add_favorite
            },
        ),
        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        style = if (isFavorite) ButtonStyle.Outlined else ButtonStyle.Normal,
        onClick = onFavoriteToggle,
        overrideColor = if (isFavorite) YummySemanticColors.StatusFavorite else null,
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
            ),
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
            DetailsButtonAction.COMMENTS,
            if (details.commentsCount > 0) {
                stringResource(R.string.details_comments_count, details.commentsCount.toCompactCount())
            } else {
                stringResource(R.string.details_comments)
            },
            Icons.AutoMirrored.Filled.Comment,
            ButtonStyle.Normal,
            onCommentsSelected,
            badge = details.commentsCount.takeIf { it > 0 }?.toCompactCount(),
        ),
        ButtonData(
            DetailsButtonAction.REVIEWS,
            if (details.reviewsCount > 0) {
                stringResource(
                    R.string.details_reviews_count,
                    details.reviewsCount,
                )
            } else {
                stringResource(R.string.details_reviews)
            },
            Icons.Filled.RateReview,
            ButtonStyle.Normal,
            onReviewsSelected,
            badge = details.reviewsCount.takeIf { it > 0 }?.toCompactCount(),
        ),
        ButtonData(
            DetailsButtonAction.BLOGGER_VIDEOS,
            stringResource(R.string.details_blogger_videos),
            Icons.Filled.VideoCameraFront,
            ButtonStyle.Normal,
            onBloggerVideosSelected,
        ),
    ) + if (hasScreenshots) {
        listOf(
            ButtonData(
                DetailsButtonAction.SCREENSHOTS,
                stringResource(R.string.details_screenshots_title),
                Icons.Filled.PhotoLibrary,
                ButtonStyle.Normal,
                onScreenshotsSelected,
            ),
        )
    } else {
        emptyList()
    }
    val availableButtons = (listOf(watchButton, libraryButton, favoriteButton) + secondaryButtons)
        .associateBy { it.action }
    val buttons = DetailsButtonAction.defaultOrder
        .filterNot { it in hiddenActions }
        .mapNotNull { availableButtons[it] }
    // Главные действия — крупными пилюлями («Любимое» — одним сердечком). Во втором ряду сначала
    // пилюли с текстом («Серии», «Подписки»), затем круглые иконки с подписью при фокусе.
    val primaryButtons = buttons.withIndex().filter { it.value.action in PrimaryActions }
    val secondRowButtons = buttons.withIndex()
        .filterNot { it.value.action in PrimaryActions }
        .sortedBy { it.value.action !in LabeledActions }
    val initialFocusedAction = buttons.firstOrNull()?.action ?: DetailsButtonAction.WATCH
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
    val requestFocusedButton: () -> Unit = {
        val targetIndex = focusedButtonIndex().coerceIn(0, buttons.lastIndex)
        scope.launch {
            focusRequesters.getOrNull(targetIndex)?.let { requestFocusUntilTimeout(it) }
        }
    }

    LaunchedEffect(buttons.size) {
        if (focusOnResume) requestFocusedButton()
    }

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            requestFocusedButton()
        }
    }

    DisposableEffect(lifecycleOwner, buttons.size, focusedAction, focusOnResume) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && focusOnResume) {
                requestFocusedButton()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var iconRowFocused by remember { mutableStateOf(false) }
    val hasIconButtons = secondRowButtons.any { it.value.action !in LabeledActions }
    // У пилюль текст и так виден — подпись только для круглых иконок.
    val iconCaption = buttons.getOrNull(currentFocusedIndex)
        ?.takeIf { iconRowFocused && it.action !in LabeledActions }
        ?.label
        .orEmpty()
    // Из нижнего ряда «вниз» ведёт прямо на чипы: поиск фокуса сам их не находит.
    val bottomDown = bottomFocusRequester?.let { Modifier.focusProperties { down = it } } ?: Modifier
    // Геометрический поиск вниз застревает в скролл-контейнере второго ряда — ведём явно на его первую кнопку.
    val primaryDown = secondRowButtons.firstOrNull()
        ?.let { first -> Modifier.focusProperties { down = focusRequesters[first.index] } }
        ?: bottomDown

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PrimaryToIconsSpacing),
    ) {
        if (primaryButtons.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // i = W*(s-1)/(2s): при ширине ряда W-2i и масштабе s выступ сфокусированной кнопки
                // не вылезает за край колонки.
                val focusInset = maxWidth * ((PrimaryFocusedScale - 1f) / (2f * PrimaryFocusedScale))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Справа крайняя кнопка — квадратная иконка: в фокусе она выступает всего на пару dp.
                        .padding(start = focusInset, end = IconEndFocusPadding)
                        .height(PrimaryRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(PrimaryButtonSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    primaryButtons.forEach { (index, button) ->
                        key(button.action) {
                            DetailsPrimaryButton(
                                button = button,
                                focusedScale = PrimaryFocusedScale,
                                onClick = {
                                    focusedAction = button.action
                                    button.onClick()
                                },
                                iconOnly = button.action == DetailsButtonAction.FAVORITE,
                                modifier = Modifier
                                    .then(
                                        when (button.action) {
                                            DetailsButtonAction.FAVORITE -> Modifier.aspectRatio(1f)
                                            DetailsButtonAction.WATCH -> Modifier.weight(WatchWeight)
                                            else -> Modifier.weight(1f)
                                        },
                                    )
                                    .then(primaryDown)
                                    .then(
                                        if (button.action == DetailsButtonAction.WATCH) {
                                            Modifier.testTag("watch_button")
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .focusRequester(focusRequesters[index])
                                    .then(
                                        if (secondRowButtons.isEmpty() && index == primaryButtons.first().index) {
                                            Modifier.focusRequester(lastRowEntryFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .onFocusChanged { if (it.isFocused) focusedAction = button.action },
                            )
                        }
                    }
                }
            }
        }

        if (secondRowButtons.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { iconRowFocused = it.hasFocus }
                        .horizontalScroll(rememberScrollState())
                        // Снизу хватает меньшего запаса: увеличенная кнопка выступает всего на пару dp.
                        .padding(
                            start = IconRowFocusPadding,
                            end = IconRowFocusPadding,
                            top = IconRowFocusPadding,
                            bottom = IconRowBottomFocusPadding,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(IconButtonSpacing),
                ) {
                    secondRowButtons.forEach { (index, button) ->
                        key(button.action) {
                            val onClick = {
                                focusedAction = button.action
                                button.onClick()
                            }
                            val focusModifier = bottomDown
                                .focusRequester(focusRequesters[index])
                                .then(
                                    if (index == secondRowButtons.first().index) {
                                        Modifier.focusRequester(lastRowEntryFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .onFocusChanged { if (it.isFocused) focusedAction = button.action }
                            if (button.action in LabeledActions) {
                                DetailsLabeledChipButton(button = button, onClick = onClick, modifier = focusModifier)
                            } else {
                                DetailsIconButton(button = button, onClick = onClick, modifier = focusModifier)
                            }
                        }
                    }
                }
                // Строка подписи нужна только круглым иконкам: у одних пилюль её место было бы пустой дырой.
                if (hasIconButtons) {
                    Text(
                        text = iconCaption,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Высота держится и без подписи, чтобы ряд не прыгал при уходе фокуса.
                        minLines = 1,
                        modifier = Modifier.padding(start = IconRowFocusPadding),
                    )
                }
            }
        }
    }
}

private val PrimaryActions = setOf(
    DetailsButtonAction.WATCH,
    DetailsButtonAction.LIBRARY,
    DetailsButtonAction.FAVORITE,
)
private val LabeledActions = setOf(
    DetailsButtonAction.EPISODES,
    DetailsButtonAction.SUBSCRIPTIONS,
)
private val PrimaryRowHeight = 52.dp
private val PrimaryButtonSpacing = 12.dp
private val PrimaryToIconsSpacing = 10.dp
private val IconButtonSpacing = 14.dp
private val IconRowFocusPadding = 8.dp
private val IconRowBottomFocusPadding = 4.dp
private val IconEndFocusPadding = 2.dp
private const val PrimaryFocusedScale = 1.04f
private const val WatchWeight = 1.6f
