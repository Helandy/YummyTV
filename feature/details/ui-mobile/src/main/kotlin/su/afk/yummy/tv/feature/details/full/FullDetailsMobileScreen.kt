package su.afk.yummy.tv.feature.details.full

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMetaRow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FullDetailsMobileScreen(
    state: FullDetailsState.State,
    effect: Flow<FullDetailsState.Effect>,
    onEvent: (FullDetailsState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = state.details?.title ?: stringResource(R.string.details_mobile_description),
                onBack = { onEvent(FullDetailsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(FullDetailsState.Event.RetrySelected) },
            empty = state.details == null,
        ) {
            val details = state.details
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text(details?.description.orEmpty()) }
                item {
                    MobileMetaRow(
                        stringResource(R.string.details_mobile_other_titles),
                        details?.otherTitles.orEmpty().joinToString(),
                    )
                }
                item {
                    MobileMetaRow(
                        stringResource(R.string.details_mobile_studios),
                        details?.studios.orEmpty().joinToString { it.title },
                    )
                }
                item {
                    MobileMetaRow(
                        stringResource(R.string.details_mobile_creators),
                        details?.creators.orEmpty().joinToString { it.title },
                    )
                }
            }
        }
    }
}
