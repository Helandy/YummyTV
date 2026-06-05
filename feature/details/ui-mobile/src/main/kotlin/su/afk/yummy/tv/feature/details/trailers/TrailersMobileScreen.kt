package su.afk.yummy.tv.feature.details.trailers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.trailers.view.TrailerMobileCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrailersMobileScreen(

    state: TrailersState.State,
    effect: Flow<TrailersState.Effect>,
    onEvent: (TrailersState.Event) -> Unit,

) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_trailers),
                onBack = { onEvent(TrailersState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(isLoading = state.isLoading, error = null, empty = state.trailers.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.trailers) { index, trailer ->
                    TrailerMobileCard(
                        number = index + 1,
                        trailer = trailer,
                    )
                }
            }
        }
    }
}
