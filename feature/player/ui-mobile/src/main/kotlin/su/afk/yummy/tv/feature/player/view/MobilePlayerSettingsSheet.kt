package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.mobile.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsSheet(
    mode: MobilePlayerSettingsMode,
    qualities: List<String>,
    selectedQuality: String?,
    onQualitySelected: (String) -> Unit,
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    dubbingNames: List<String>,
    dubbingEpisodeCounts: List<Int>,
    dubbingViews: List<Int>,
    dubbingSourceNames: List<String>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTrackTab by rememberSaveable {
        mutableStateOf(MobilePlayerTrackSettingsTab.Dubbing)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = when (mode) {
                        MobilePlayerSettingsMode.Track -> stringResource(UiR.string.player_mobile_track_settings_title)
                        MobilePlayerSettingsMode.Playback -> stringResource(UiR.string.player_mobile_playback_settings_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            if (mode == MobilePlayerSettingsMode.Playback) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (qualities.isNotEmpty()) {
                            MobilePlayerSettingsSection(
                                title = stringResource(UiR.string.player_mobile_quality),
                                modifier = Modifier.weight(1f),
                            ) {
                                qualities.forEach { quality ->
                                    MobilePlayerSelectionRow(
                                        label = quality,
                                        selected = quality == selectedQuality,
                                        onClick = { onQualitySelected(quality) },
                                    )
                                }
                            }
                        }
                        MobilePlayerSettingsSection(
                            title = stringResource(UiR.string.player_mobile_speed),
                            modifier = Modifier.weight(1f),
                        ) {
                            speeds.forEach { speed ->
                                val label = "${speed}x"
                                MobilePlayerSelectionRow(
                                    label = label,
                                    selected = speed == selectedSpeed,
                                    onClick = { onSpeedSelected(speed) },
                                )
                            }
                        }
                    }
                }
            }
            if (mode == MobilePlayerSettingsMode.Track) {
                item {
                    MobilePlayerTrackSettingsTabs(
                        selectedTab = selectedTrackTab,
                        dubbingLabel = stringResource(UiR.string.player_mobile_dubbing),
                        playerLabel = stringResource(UiR.string.player_mobile_player),
                        onTabSelected = { selectedTrackTab = it },
                    )
                }
                when (selectedTrackTab) {
                    MobilePlayerTrackSettingsTab.Dubbing -> item {
                        MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_dubbing)) {
                            dubbingNames.forEachIndexed { index, name ->
                                MobilePlayerSelectionRow(
                                    label = name,
                                    selected = index == selectedDubbingIndex,
                                    metaContent = { contentColor ->
                                        MobilePlayerDubbingMeta(
                                            views = dubbingViews.getOrElse(index) { 0 },
                                            episodeCount = dubbingEpisodeCounts.getOrElse(index) { 0 },
                                            sourceNames = dubbingSourceNames.getOrElse(index) { "" },
                                            contentColor = contentColor,
                                        )
                                    },
                                    onClick = { onDubbingSelected(index) },
                                )
                            }
                        }
                    }

                    MobilePlayerTrackSettingsTab.Player -> item {
                        MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_player)) {
                            balancerNames.forEachIndexed { index, name ->
                                MobilePlayerSelectionRow(
                                    label = name,
                                    selected = index == selectedBalancerIndex,
                                    onClick = { onBalancerSelected(index) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobilePlayerSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun MobilePlayerSelectionRow(
    label: String,
    selected: Boolean,
    metaContent: @Composable ColumnScope.(contentColor: Color) -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            metaContent(textColor)
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = textColor)
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun MobilePlayerDubbingMeta(
    views: Int,
    episodeCount: Int,
    sourceNames: String,
    contentColor: Color,
) {
    val metaColor = contentColor.copy(alpha = 0.68f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Visibility,
            contentDescription = null,
            tint = metaColor,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = views.formatCompactCount(),
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = metaColor,
            modifier = Modifier
                .padding(start = 7.dp)
                .size(13.dp),
        )
        Text(
            text = episodeCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
            maxLines = 1,
        )
    }
    if (sourceNames.isNotBlank()) {
        Text(
            text = sourceNames,
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
