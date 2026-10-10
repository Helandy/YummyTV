package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Alloha reports dubbings and subtitles together, so one panel carries both as sections - the same
 * shape the mobile sheet uses - instead of two sibling panels behind two separate buttons.
 */
@Composable
internal fun TvPlayerAllohaPanel(
    visible: Boolean,
    audioTrackNames: List<String>,
    selectedAudioTrackIndex: Int,
    onAudioTrackSelected: (index: Int) -> Unit,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onSubtitleTrackSelected: (index: Int) -> Unit,
    selectedFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onExitDown: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible && (audioTrackNames.isNotEmpty() || subtitleTrackNames.isNotEmpty()),
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
        val listState = rememberLazyListState()
        // Header rows shift the audio list down by one; scroll so the current pick is on screen.
        val scrollIndex = (selectedAudioTrackIndex + 1)
            .coerceIn(0, audioTrackNames.size)

        LaunchedEffect(visible, audioTrackNames.size, scrollIndex) {
            if (visible) listState.scrollToItem(scrollIndex)
        }

        Column(
            modifier = Modifier
                .width(336.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(YummySemanticColors.PanelScrim)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .heightIn(max = maxPanelHeight)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (audioTrackNames.isNotEmpty()) {
                    item(key = "audio-header") {
                        PanelSectionHeader(stringResource(R.string.player_dubbing_title))
                    }
                    itemsIndexed(audioTrackNames, keyPrefix = "audio") { index, label ->
                        val selected = index == selectedAudioTrackIndex
                        PlayerSelectionItem(
                            label = label,
                            metaContent = {},
                            selected = selected,
                            enabled = true,
                            modifier = if (selected) {
                                Modifier.focusRequester(selectedFocusRequester)
                            } else {
                                Modifier
                            },
                            onExitDown = null,
                            onClick = { onAudioTrackSelected(index) },
                        )
                    }
                }
                if (subtitleTrackNames.isNotEmpty()) {
                    item(key = "subtitle-header") {
                        PanelSectionHeader(stringResource(R.string.player_subtitles_title))
                    }
                    itemsIndexed(subtitleTrackNames, keyPrefix = "subtitle") { index, label ->
                        PlayerSelectionItem(
                            label = label,
                            metaContent = {},
                            selected = index == selectedSubtitleTrackIndex,
                            enabled = true,
                            modifier = Modifier,
                            onExitDown = if (index == subtitleTrackNames.lastIndex) {
                                onExitDown
                            } else {
                                null
                            },
                            onClick = { onSubtitleTrackSelected(index) },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.itemsIndexed(
    values: List<String>,
    keyPrefix: String,
    itemContent: @Composable (index: Int, label: String) -> Unit,
) = items(
    count = values.size,
    key = { index -> "$keyPrefix-$index-${values[index]}" },
) { index -> itemContent(index, values[index]) }

@Composable
private fun PanelSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.62f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
