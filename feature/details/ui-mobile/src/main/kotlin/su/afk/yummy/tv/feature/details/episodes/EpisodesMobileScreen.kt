package su.afk.yummy.tv.feature.details.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.view.BalancerDialog
import su.afk.yummy.tv.feature.details.episodes.utils.mobileWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.toMobileEpisodeGroups
import su.afk.yummy.tv.feature.details.episodes.view.EpisodeMobileCard
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodesMobileScreen(

    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,

) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_episodes),
                onBack = { onEvent(EpisodesState.Event.BackSelected) },
            )
        },
    ) {
        val content = state.videosState as? VideosUiState.Content
        val episodeGroups = remember(content?.videos) {
            content?.videos.orEmpty().toMobileEpisodeGroups()
        }
        MobileStateContent(
            isLoading = state.videosState is VideosUiState.Loading,
            error = null,
            empty = state.videosState is VideosUiState.Empty,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(episodeGroups, key = { it.episode }) { group ->
                    EpisodeMobileCard(
                        video = group.video,
                        watchStatus = group.videos.mobileWatchStatus(state.watchProgress),
                        kodikIframeUrl = group.kodikIframeUrl,
                        onInfoClick = {
                            onEvent(EpisodesState.Event.EpisodeDubbingsSelected(group.episode))
                        },
                        onClick = { onEvent(EpisodesState.Event.VideoSelected(group.video)) },
                    )
                }
            }
        }
    }

    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = { onEvent(EpisodesState.Event.BalancerConfirmed(it)) },
            onDismiss = { onEvent(EpisodesState.Event.BalancerPickerDismissed) },
        )
    }
}
