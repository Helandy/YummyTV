package su.afk.yummy.tv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.view.ContinueWatchingSection
import su.afk.yummy.tv.feature.home.view.HomeFeedSectionRow
import su.afk.yummy.tv.feature.home.view.HomeHeroCarousel

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
