package su.afk.yummy.tv.feature.player.mobile.view

import su.afk.yummy.tv.core.designsystem.components.ScrollToTopOnChange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheetCustom
import su.afk.yummy.tv.core.designsystem.baseScreen.HideSheetWindowSystemBars
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerDubbingSheet(
    playback: PlayerPlaybackUiState,
    onDubbingSelected: (Int) -> Unit,
    onDubbingStarClick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    ScrollToTopOnChange(order = playback.dubbingNames.toList(), state = scrollState)
    BaseBottomSheetCustom(onDismissRequest = onDismiss) { maxHeight ->
        HideSheetWindowSystemBars()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.player_dubbing_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            playback.dubbingNames.forEachIndexed { index, name ->
                val enabled = playback.dubbingAvailability.getOrElse(index) { true }
                MobilePlayerDubbingRow(
                    label = name,
                    selected = index == playback.currentDubbingIndex,
                    enabled = enabled,
                    views = playback.dubbingViews.getOrElse(index) { 0 },
                    episodeCount = playback.dubbingEpisodeCounts.getOrElse(index) { 0 },
                    sourceNames = playback.dubbingSourceNames.getOrElse(index) { "" },
                    starred = playback.dubbingStarred.getOrElse(index) { false },
                    onStarClick = { onDubbingStarClick(index) },
                    onClick = { onDubbingSelected(index) },
                )
            }
        }
    }
}
