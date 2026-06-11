package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.episodes.dubbings.view.EpisodeDubbingMobileRow
import su.afk.yummy.tv.feature.details.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDubbingsMobileScreen(
    state: EpisodeDubbingsState.State,
    effect: Flow<EpisodeDubbingsState.Effect>,
    onEvent: (EpisodeDubbingsState.Event) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(EpisodeDubbingsState.Event.BackSelected) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                state.dubbings.isEmpty() -> Text(
                    text = stringResource(R.string.details_mobile_episode_dubbings_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.dubbings, key = { it }) { dubbing ->
                        EpisodeDubbingMobileRow(dubbing = dubbing)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
