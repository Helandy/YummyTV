package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.episodes.dubbings.view.EpisodeDubbingMobileRow
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodeDubbingsMobileScreen(

    state: EpisodeDubbingsState.State,
    effect: Flow<EpisodeDubbingsState.Effect>,
    onEvent: (EpisodeDubbingsState.Event) -> Unit,

    ) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(
                    R.string.details_mobile_episode_dubbings_title,
                    state.episode
                ),
                onBack = { onEvent(EpisodeDubbingsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            empty = state.dubbings.isEmpty(),
            emptyText = stringResource(R.string.details_mobile_episode_dubbings_empty),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.dubbings, key = { it }) { dubbing ->
                    EpisodeDubbingMobileRow(dubbing = dubbing)
                }
            }
        }
    }
}
