package su.afk.yummy.tv.feature.details.mobile.episodes.dubbings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.episodes.dubbings.view.EpisodeDubbingMobileRow
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun EpisodeDubbingsMobileScreenDefaultPreview() = ScreenPreviewTheme {
    EpisodeDubbingsMobileScreen(EpisodeDubbingsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun EpisodeDubbingsMobileScreenLoadingPreview() = ScreenPreviewTheme {
    EpisodeDubbingsMobileScreen(EpisodeDubbingsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun EpisodeDubbingsMobileScreenErrorPreview() = ScreenPreviewTheme {
    EpisodeDubbingsMobileScreen(
        EpisodeDubbingsState.State(
            isLoading = false,
            error = "Не удалось загрузить озвучки"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodeDubbingsMobileScreen(
    state: EpisodeDubbingsState.State,
    effect: Flow<EpisodeDubbingsState.Effect>,
    onEvent: (EpisodeDubbingsState.Event) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = { onEvent(EpisodeDubbingsState.Event.BackSelected) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.details_mobile_episode_dubbings_title,
                    state.episode
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.error != null -> MobileMessage(
                    title = state.error.orEmpty(),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(CoreR.string.retry),
                    onAction = { onEvent(EpisodeDubbingsState.Event.RetrySelected) },
                    fillMaxSize = false,
                )

                state.dubbings.isEmpty() -> MobileMessage(
                    title = stringResource(R.string.details_mobile_episode_dubbings_empty),
                    fillMaxSize = false,
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.dubbings, key = { it.name }) { dubbing ->
                        EpisodeDubbingMobileRow(
                            dubbing = dubbing,
                            onClick = {
                                onEvent(EpisodeDubbingsState.Event.DubbingSelected(dubbing.name))
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
