package su.afk.yummy.tv.feature.details.rating

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.rating.view.MobileListStats
import su.afk.yummy.tv.feature.details.rating.view.MobileRatingDistribution
import su.afk.yummy.tv.feature.details.rating.view.MobileRatingOverview
import su.afk.yummy.tv.feature.details.rating.view.MobileRatingPicker

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RatingMobileScreenDefaultPreview() = ScreenPreviewTheme {
    RatingMobileScreen(RatingState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun RatingMobileScreenLoadingPreview() = ScreenPreviewTheme {
    RatingMobileScreen(RatingState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun RatingMobileScreenErrorPreview() = ScreenPreviewTheme {
    RatingMobileScreen(
        RatingState.State(isLoading = false, error = "Не удалось загрузить рейтинг"),
        emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RatingMobileScreen(
    state: RatingState.State,
    effect: Flow<RatingState.Effect>,
    onEvent: (RatingState.Event) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is RatingState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_rating),
                onBack = { onEvent(RatingState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(RatingState.Event.RetrySelected) },
        ) {
            LazyColumn(
                modifier = Modifier.navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    MobileRatingOverview(
                        ratingSummary = state.ratingSummary,
                        selectedUserRating = state.selectedUserRating
                            ?: state.ratingSummary.userRating,
                    )
                }
                item {
                    MobileRatingPicker(
                        selectedRating = state.selectedUserRating ?: state.ratingSummary.userRating,
                        onRatingSelected = { rating ->
                            onEvent(
                                RatingState.Event.RatingSelected(
                                    rating
                                )
                            )
                        },
                    )
                }
                if (state.selectedUserRating != null || state.ratingSummary.userRating != null) {
                    item {
                        Button(
                            onClick = { onEvent(RatingState.Event.RatingDeleted) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.details_mobile_delete_rating))
                        }
                    }
                }
                item {
                    MobileRatingDistribution(distribution = state.ratingSummary.distribution)
                }
                item {
                    MobileListStats(listStats = state.listStats)
                }
            }
        }
    }
}
