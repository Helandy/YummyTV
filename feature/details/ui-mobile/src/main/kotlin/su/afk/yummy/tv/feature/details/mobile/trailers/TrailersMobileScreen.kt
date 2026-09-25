package su.afk.yummy.tv.feature.details.mobile.trailers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.trailers.view.TrailerMobileCard
import su.afk.yummy.tv.feature.details.trailers.TrailersState

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrailersMobileScreenDefaultPreview() = ScreenPreviewTheme {
    TrailersMobileScreen(TrailersState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TrailersMobileScreen(

    state: TrailersState.State,
    effect: Flow<TrailersState.Effect>,
    onEvent: (TrailersState.Event) -> Unit,

    ) {
    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_trailers),
                onBack = { onEvent(TrailersState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(TrailersState.Event.RetrySelected) },
            empty = state.trailers.isEmpty()
        ) {
            LazyColumn(
                modifier = Modifier.mobileContentMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    state.trailers,
                    key = { _, trailer -> trailer.iframeUrl },
                ) { index, trailer ->
                    TrailerMobileCard(
                        number = index + 1,
                        trailer = trailer,
                    )
                }
            }
        }
    }
}
