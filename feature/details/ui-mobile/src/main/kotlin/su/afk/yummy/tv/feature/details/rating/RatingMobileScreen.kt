package su.afk.yummy.tv.feature.details.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun RatingMobileScreen(
    state: RatingState.State,
    effect: Flow<RatingState.Effect>,
    onEvent: (RatingState.Event) -> Unit,
) {
    DetailsMobileScaffold(
        title = stringResource(R.string.details_mobile_rating),
        onBack = { onEvent(RatingState.Event.BackSelected) },
    ) { padding ->
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(RatingState.Event.RetrySelected) },
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(
                            R.string.details_mobile_user_rating,
                            state.selectedUserRating ?: state.ratingSummary.userRating ?: "-",
                        ),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..10).forEach { rating ->
                            Button(onClick = { onEvent(RatingState.Event.RatingSelected(rating)) }) {
                                Text(rating.toString())
                            }
                        }
                    }
                }
                item {
                    Button(onClick = { onEvent(RatingState.Event.RatingDeleted) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.details_mobile_delete_rating))
                    }
                }
                item {
                    Text(state.ratingSummary.distribution.joinToString("\n") { "${it.rating}: ${it.count}" })
                }
            }
        }
    }
}
