package su.afk.yummy.tv.feature.details.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.feature.details.mobile.R
import java.util.Locale

private data class MobileDetailsAction(
    val action: DetailsButtonAction,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun DetailsMobileScreen(
    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,
) {
    val details = state.details
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Box(Modifier.fillMaxSize()) {
            MobileStateContent(
                isLoading = state.isLoading && details == null,
                error = state.error.takeIf { details == null },
                onRetry = { onEvent(DetailsState.Event.RetrySelected) },
                empty = details == null,
                emptyText = stringResource(R.string.details_mobile_empty),
            ) {
                if (details != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        item(key = "hero") {
                            DetailsMobileHero(
                                state = state,
                                details = details,
                                onBack = { onEvent(DetailsState.Event.BackSelected) },
                                onPosterClick = { onEvent(DetailsState.Event.PosterClicked) },
                                onWatchSelected = { onEvent(DetailsState.Event.WatchSelected) },
                                onLibraryToggle = { onEvent(DetailsState.Event.LibraryToggled) },
                                onFavoriteToggle = { onEvent(DetailsState.Event.FavoriteToggled) },
                            )
                        }
                        item(key = "actions") {
                            DetailsSecondaryActions(
                                state = state,
                                details = details,
                                onSubscriptionsSelected = { onEvent(DetailsState.Event.SubscriptionsSelected) },
                                onFullDetailsSelected = { onEvent(DetailsState.Event.FullDetailsSelected) },
                                onEpisodesSelected = { onEvent(DetailsState.Event.EpisodesSelected) },
                                onTrailersSelected = { onEvent(DetailsState.Event.TrailersSelected) },
                                onSimilarSelected = { onEvent(DetailsState.Event.SimilarSelected) },
                                onViewingOrderSelected = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                                onScreenshotsSelected = { onEvent(DetailsState.Event.ScreenshotsSelected) },
                                onRatingScreenSelected = { onEvent(DetailsState.Event.RatingScreenSelected) },
                                onCollectionsSelected = { onEvent(DetailsState.Event.CollectionsSelected) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        item(key = "description") {
                            DetailsDescriptionSection(
                                description = details.description,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        state.error?.let { error ->
                            item(key = "soft_error") {
                                MobileMessage(
                                    title = error,
                                    actionLabel = stringResource(R.string.details_mobile_retry),
                                    onAction = { onEvent(DetailsState.Event.RetrySelected) },
                                )
                            }
                        }
                    }
                }
            }

            DetailsPickerSheets(
                state = state,
                onLibraryListSelected = { onEvent(DetailsState.Event.LibraryListSelected(it)) },
                onLibraryDismiss = { onEvent(DetailsState.Event.LibraryListPickerDismissed) },
                onSubscriptionToggle = { onEvent(DetailsState.Event.SubscriptionToggled(it)) },
                onSubscriptionsDismiss = { onEvent(DetailsState.Event.SubscriptionsDismissed) },
                onBalancerConfirmed = { onEvent(DetailsState.Event.BalancerConfirmed(it)) },
                onBalancerDismiss = { onEvent(DetailsState.Event.BalancerPickerDismissed) },
            )

            if (state.showPosterFullscreen && details != null) {
                PosterDialog(
                    details = details,
                    onDismiss = { onEvent(DetailsState.Event.PosterDismissed) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsMobileHero(
    state: DetailsState.State,
    details: AnimeDetails,
    onBack: () -> Unit,
    onPosterClick: () -> Unit,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.82f).coerceAtLeast(620.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = details.poster.bestUrl(),
            contentDescription = details.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.36f to Color.Black.copy(alpha = 0.18f),
                            0.58f to MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            0.82f to MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            1f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(heroHeight * 0.45f)
                .clickable(onClick = onPosterClick),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(1f)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.46f)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.details_mobile_back),
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DetailsRatingRow(details)
            Text(
                text = details.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            DetailsMetaChips(details)
            details.genres.take(5).joinToString(" • ") { it.title }.takeIf { it.isNotBlank() }?.let { genres ->
                Text(
                    text = genres,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            details.episodes?.formatAiredProgress()?.let { progress ->
                Text(
                    text = progress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                )
            }
            DetailsPrimaryActions(
                state = state,
                details = details,
                onWatchSelected = onWatchSelected,
                onLibraryToggle = onLibraryToggle,
                onFavoriteToggle = onFavoriteToggle,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun DetailsPrimaryActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchLabel = state.watchLabel(details)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onWatchSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = watchLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onLibraryToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            ) {
                Icon(
                    imageVector = if (state.isInLibrary) {
                        Icons.AutoMirrored.Filled.PlaylistAddCheck
                    } else {
                        Icons.AutoMirrored.Filled.PlaylistAdd
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.libraryLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (state.isFavorite) R.string.details_mobile_favorite_on
                        else R.string.details_mobile_favorite_off,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsSecondaryActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildMobileActions(
        state = state,
        details = details,
        onSubscriptionsSelected = onSubscriptionsSelected,
        onFullDetailsSelected = onFullDetailsSelected,
        onEpisodesSelected = onEpisodesSelected,
        onTrailersSelected = onTrailersSelected,
        onSimilarSelected = onSimilarSelected,
        onViewingOrderSelected = onViewingOrderSelected,
        onScreenshotsSelected = onScreenshotsSelected,
        onRatingScreenSelected = onRatingScreenSelected,
        onCollectionsSelected = onCollectionsSelected,
    )
    if (actions.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.details_mobile_sections),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            actions.forEach { action ->
                DetailsActionCard(
                    action = action,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                )
            }
            if (actions.size % 2 != 0) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailsActionCard(
    action: MobileDetailsAction,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = action.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsMetaChips(details: AnimeDetails) {
    val chips = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.takeIf { it.isNotBlank() }?.let { add(it) }
        details.status?.takeIf { it.isNotBlank() }?.let { add(it) }
        details.ageRating?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    if (chips.isEmpty() && details.views == null) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            DetailsChip(label = chip)
        }
        details.views?.let { views ->
            DetailsChip(
                label = views.formatViews(),
                icon = Icons.Filled.Visibility,
            )
        }
    }
}

@Composable
private fun DetailsChip(
    label: String,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f),
                shape = RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsRatingRow(details: AnimeDetails) {
    val labels = buildList {
        details.rating.average?.let {
            add(RatingLabel(stringResource(R.string.details_mobile_yani_rating, it.formatRating()), true))
        }
        details.rating.kinopoisk?.let {
            add(RatingLabel(stringResource(R.string.details_mobile_kinopoisk_rating, it.formatRating()), false))
        }
        details.rating.shikimori?.let { add(RatingLabel("Shikimori ${it.formatRating()}", false)) }
        details.rating.myAnimeList?.let { add(RatingLabel("MAL ${it.formatRating()}", false)) }
    }
    if (labels.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { item ->
            Row(
                modifier = Modifier
                    .background(
                        color = if (item.isPrimary) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.46f),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (item.isPrimary) MaterialTheme.colorScheme.onPrimary else Color(0xFFFFC857),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (item.isPrimary) MaterialTheme.colorScheme.onPrimary else Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DetailsDescriptionSection(
    description: String,
    modifier: Modifier = Modifier,
) {
    if (description.isBlank()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.details_mobile_description),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun DetailsPickerSheets(
    state: DetailsState.State,
    onLibraryListSelected: (UserAnimeList) -> Unit,
    onLibraryDismiss: () -> Unit,
    onSubscriptionToggle: (String) -> Unit,
    onSubscriptionsDismiss: () -> Unit,
    onBalancerConfirmed: (su.afk.yummy.tv.domain.anime.model.AnimeVideo) -> Unit,
    onBalancerDismiss: () -> Unit,
) {
    if (state.showLibraryListPicker) {
        LibraryListDialog(
            onSelected = onLibraryListSelected,
            onDismiss = onLibraryDismiss,
        )
    }
    if (state.showSubscriptionsPicker) {
        SubscriptionsDialog(
            state = state,
            onToggle = onSubscriptionToggle,
            onDismiss = onSubscriptionsDismiss,
        )
    }
    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = onBalancerConfirmed,
            onDismiss = onBalancerDismiss,
        )
    }
}

@Composable
private fun LibraryListDialog(
    onSelected: (UserAnimeList) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        UserAnimeList.WATCHING,
        UserAnimeList.PLANNED,
        UserAnimeList.COMPLETED,
        UserAnimeList.POSTPONED,
        UserAnimeList.DROPPED,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_mobile_library_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    OutlinedButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.label())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_cancel))
            }
        },
    )
}

@Composable
private fun SubscriptionsDialog(
    state: DetailsState.State,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_mobile_subscriptions)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.isSubscriptionsLoading && state.subscriptions.isEmpty() -> {
                        Text(stringResource(R.string.details_mobile_subscriptions_loading))
                    }
                    state.subscriptions.isEmpty() -> {
                        Text(stringResource(R.string.details_mobile_subscriptions_empty))
                    }
                    else -> state.subscriptions.forEach { option ->
                        FilledTonalButton(
                            onClick = { onToggle(option.key) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = option.dubbing,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.details_mobile_subscription_meta,
                                        option.player,
                                        option.episodesCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (option.isSubscribed) {
                                        R.string.details_mobile_unsubscribe
                                    } else {
                                        R.string.details_mobile_subscribe
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_close))
            }
        },
    )
}

@Composable
private fun BalancerDialog(
    picker: BalancerPickerState,
    onConfirmed: (su.afk.yummy.tv.domain.anime.model.AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.details_mobile_balancer_title, picker.episodeNumber))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                picker.options.forEach { option ->
                    FilledTonalButton(
                        enabled = option.isSupported,
                        onClick = { onConfirmed(option.video) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (option.isSupported) {
                                option.playerName
                            } else {
                                stringResource(R.string.details_mobile_unsupported_player, option.playerName)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_cancel))
            }
        },
    )
}

@Composable
private fun PosterDialog(
    details: AnimeDetails,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = details.poster.bestUrl(),
                contentDescription = details.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.details_mobile_close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun buildMobileActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
): List<MobileDetailsAction> {
    val availableActions = buildList {
        add(
            MobileDetailsAction(
                DetailsButtonAction.EPISODES,
                stringResource(R.string.details_mobile_episodes),
                Icons.Filled.VideoLibrary,
                onEpisodesSelected,
            )
        )
        if (state.isSignedIn && state.subscriptions.isNotEmpty()) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SUBSCRIPTIONS,
                    stringResource(R.string.details_mobile_subscriptions),
                    Icons.Filled.Notifications,
                    onSubscriptionsSelected,
                )
            )
        }
        add(
            MobileDetailsAction(
                DetailsButtonAction.FULL_DETAILS,
                stringResource(R.string.details_mobile_full_details),
                Icons.Filled.Info,
                onFullDetailsSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.TRAILERS,
                stringResource(R.string.details_mobile_trailers),
                Icons.Filled.Movie,
                onTrailersSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.SIMILAR,
                stringResource(R.string.details_mobile_similar),
                Icons.Filled.AutoAwesome,
                onSimilarSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.VIEWING_ORDER,
                stringResource(R.string.details_mobile_viewing_order),
                Icons.Filled.FormatListNumbered,
                onViewingOrderSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.RATING,
                stringResource(R.string.details_mobile_rating),
                Icons.Filled.Star,
                onRatingScreenSelected,
            )
        )
        if (state.collections.isNotEmpty()) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.COLLECTIONS,
                    stringResource(R.string.details_mobile_collections),
                    Icons.Filled.CollectionsBookmark,
                    onCollectionsSelected,
                )
            )
        }
        if (details.screenshots.isNotEmpty()) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SCREENSHOTS,
                    stringResource(R.string.details_mobile_screenshots),
                    Icons.Filled.PhotoLibrary,
                    onScreenshotsSelected,
                )
            )
        }
    }
    val byAction = availableActions.associateBy { it.action }
    return state.detailsButtonOrder
        .filterNot { it == DetailsButtonAction.WATCH || it == DetailsButtonAction.LIBRARY || it == DetailsButtonAction.FAVORITE }
        .mapNotNull { byAction[it] } +
        availableActions.filterNot { it.action in state.detailsButtonOrder }
}

@Composable
private fun DetailsState.State.watchLabel(details: AnimeDetails): String {
    val resumeEntry = watchProgress.values
        .filter { it.animeId == details.id && it.positionMs > 0 }
        .maxByOrNull { it.updatedAt }
    return when {
        isWatchLaunchPending || videosState is VideosUiState.Loading -> {
            stringResource(R.string.details_mobile_loading_episodes)
        }
        videosState is VideosUiState.Empty -> stringResource(R.string.details_mobile_watch_not_found)
        resumeEntry != null && resumeEntry.episode.isNotBlank() -> {
            stringResource(R.string.details_mobile_continue_episode, resumeEntry.episode)
        }
        else -> stringResource(R.string.details_mobile_watch)
    }
}

@Composable
private fun DetailsState.State.libraryLabel(): String = when {
    isInLibrary -> (libraryList ?: UserAnimeList.WATCHING).label()
    else -> stringResource(R.string.details_mobile_add_library)
}

@Composable
private fun UserAnimeList.label(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.details_mobile_library_watching
        UserAnimeList.PLANNED -> R.string.details_mobile_library_planned
        UserAnimeList.COMPLETED -> R.string.details_mobile_library_completed
        UserAnimeList.POSTPONED -> R.string.details_mobile_library_postponed
        UserAnimeList.DROPPED -> R.string.details_mobile_library_dropped
    }
)

private data class RatingLabel(val label: String, val isPrimary: Boolean)

private fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

private fun Double.formatRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    return String.format(Locale.US, "%.1f", rounded)
}

private fun Int.formatViews(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000f)
    this >= 1_000 -> "${this / 1_000}K"
    else -> toString()
}

@Composable
private fun AnimeEpisodes.formatAiredProgress(): String? {
    val airedCount = aired ?: return null
    val totalCount = count?.toString() ?: stringResource(R.string.details_mobile_unknown_count)
    return stringResource(R.string.details_mobile_aired, airedCount, totalCount)
}
