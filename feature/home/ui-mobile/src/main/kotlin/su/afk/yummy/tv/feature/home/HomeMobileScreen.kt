package su.afk.yummy.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileProgressMediaCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomePoster
import java.util.Locale

@Composable
fun HomeMobileScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item ->
            when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> onEvent(HomeState.Event.AnimeSelected(action.seriesId))
                is HomeFeedItemAction.OpenVideo -> onEvent(HomeState.Event.VideoSelected(action.videoId))
                is HomeFeedItemAction.OpenCollection -> onEvent(HomeState.Event.CollectionSelected(action.collectionId))
            }
        }
    }

    MobileStateContent(
        isLoading = state.isLoading || state.feed == null || !state.isContinueWatchingLoaded,
        error = state.error,
        onRetry = { onEvent(HomeState.Event.RetrySelected) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            val feed = state.feed

            if (feed != null && feed.heroItems.isNotEmpty()) {
                item(key = "hero") {
                    HomeHeroCarousel(
                        items = feed.heroItems,
                        onItemSelected = onItemSelected,
                        onItemVisible = { onEvent(HomeState.Event.HeroItemVisible(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue_watching") {
                    ContinueWatchingSection(
                        entries = state.continueWatching,
                        onEntrySelected = { onEvent(HomeState.Event.ContinueWatchingSelected(it)) },
                    )
                }
            }

            feed?.sections
                .orEmpty()
                .filter { it.items.isNotEmpty() }
                .forEach { section ->
                    item(key = "section_${section.title}") {
                        HomeFeedSectionRow(
                            section = section,
                            onItemSelected = onItemSelected,
                        )
                    }
                }
        }
    }
}

@Composable
private fun HomeHeroCarousel(
    items: List<HomeFeedItem>,
    onItemSelected: (HomeFeedItem) -> Unit,
    onItemVisible: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { items.size }

    LaunchedEffect(pagerState.currentPage, items) {
        items.getOrNull(pagerState.currentPage)?.let { onItemVisible(it.id) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items[page].id },
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items[page]
            HomeHeroCard(
                item = item,
                onClick = { onItemSelected(item) },
            )
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, _ ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeroCard(
    item: HomeFeedItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.poster.bestUrl(),
                contentDescription = null,
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
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.rating?.let { rating ->
                    Text(
                        text = rating.asRatingBadge(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    entries: List<WatchProgressEntry>,
    onEntrySelected: (WatchProgressEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = "Продолжить просмотр",
            trailing = entries.size.toString(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { "${it.animeId}-${it.episode}-${it.videoId}" }) { entry ->
                MobileProgressMediaCard(
                    title = entry.animeTitle.ifBlank { "Эпизод ${entry.episode}" },
                    imageUrl = entry.bestImageUrl(),
                    subtitle = entry.progressSubtitle(),
                    progress = entry.watchProgress(),
                    onClick = { onEntrySelected(entry) },
                )
            }
        }
    }
}

@Composable
private fun HomeFeedSectionRow(
    section: HomeFeedSection,
    onItemSelected: (HomeFeedItem) -> Unit,
) {
    val showCardMetadata = !section.isTitleOnlySection()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = section.title,
            trailing = section.items.size.toString(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = { it.id }) { item ->
                HomeItemCard(
                    item = item,
                    showMetadata = showCardMetadata,
                    onClick = { onItemSelected(item) },
                )
            }
        }
    }
}

@Composable
private fun HomeItemCard(
    item: HomeFeedItem,
    showMetadata: Boolean,
    onClick: () -> Unit,
) {
    MobileContentPosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        description = item.description,
        rating = item.rating,
        showMetadata = showMetadata,
        onClick = onClick,
    )
}

private fun HomeFeedSection.isTitleOnlySection(): Boolean =
    title == "Новинки" ||
        title == "New releases" ||
        title == "Коллекции" ||
        title == "Collections"

private fun WatchProgressEntry.bestImageUrl(): String? =
    screenshotUrl.ifBlank { posterUrl }.ifBlank { null }

private fun WatchProgressEntry.progressSubtitle(): String =
    listOfNotNull(
        "Эпизод $episode",
        dubbing.takeIf { it.isNotBlank() },
        playerName.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

private fun WatchProgressEntry.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

private fun Double.asRatingBadge(): String = String.format(Locale.US, "%.1f", this)

private fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
