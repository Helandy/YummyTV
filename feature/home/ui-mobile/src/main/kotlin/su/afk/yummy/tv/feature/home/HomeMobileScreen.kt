package su.afk.yummy.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.mobile.R
import su.afk.yummy.tv.feature.home.view.ContinueWatchingSection
import su.afk.yummy.tv.feature.home.view.HomeFeedSectionRow
import su.afk.yummy.tv.feature.home.view.HomeHeroCarousel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeMobileScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    val mainActions = LocalMobileMainActions.current
    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item ->
            when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> onEvent(HomeState.Event.AnimeSelected(action.seriesId))
                is HomeFeedItemAction.OpenVideo -> onEvent(HomeState.Event.VideoSelected(action.videoId))
                is HomeFeedItemAction.OpenCollection -> onEvent(HomeState.Event.CollectionSelected(action.collectionId))
            }
        }
    }

    val title = stringResource(R.string.home_mobile_title)
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = title,
                actions = {
                    if (mainActions != null) {
                        IconButton(onClick = mainActions.onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                        IconButton(onClick = mainActions.onAccountClick) {
                            BadgedBox(
                                badge = {
                                    if (mainActions.unreadNotificationsCount > 0) {
                                        Badge { Text(mainActions.unreadNotificationsCount.toString()) }
                                    }
                                },
                            ) {
                                AccountAvatarIcon(avatarUrl = mainActions.avatarUrl)
                            }
                        }
                    }
                },
            )
        },
        isLoading = state.isLoading || state.feed == null || !state.isContinueWatchingLoaded,
        error = state.error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(HomeState.Event.RetrySelected) },
        errorContent = state.error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.home_mobile_retry),
                    onAction = retry,
                )
            }
        },
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
private fun AccountAvatarIcon(avatarUrl: String) {
    if (avatarUrl.isNotBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        )
    } else {
        Icon(Icons.Default.AccountCircle, contentDescription = null)
    }
}
