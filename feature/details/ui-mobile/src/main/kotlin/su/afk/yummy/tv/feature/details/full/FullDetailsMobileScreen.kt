package su.afk.yummy.tv.feature.details.full

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMetaRow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.mobile.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FullDetailsMobileScreenDefaultPreview() = ScreenPreviewTheme {
    FullDetailsMobileScreen(FullDetailsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun FullDetailsMobileScreenLoadingPreview() = ScreenPreviewTheme {
    FullDetailsMobileScreen(FullDetailsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun FullDetailsMobileScreenErrorPreview() = ScreenPreviewTheme {
    FullDetailsMobileScreen(
        FullDetailsState.State(
            isLoading = false,
            error = "Не удалось загрузить описание"
        ), emptyFlow()
    ) {}
}

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
                title = stringResource(R.string.details_mobile_description),
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
                modifier = Modifier.navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                details?.title?.takeIf { it.isNotBlank() }?.let { title ->
                    item {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                details?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item { Text(description) }
                }
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
