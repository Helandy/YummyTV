package su.afk.yummy.tv.feature.player.mobile.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Выезжающая справа панель настроек плеера: качество, скорость, масштаб, озвучка Alloha, дорожка и оформление
 * субтитров, громкость и стабилизация. Всё применяется сразу — глобальные настройки общие с экраном
 * «Настройки». Тап мимо панели закрывает её.
 */
@Composable
internal fun BoxScope.MobilePlayerSidePanel(
    visible: Boolean,
    qualities: List<String>,
    selectedQuality: String?,
    onQualitySelected: (String) -> Unit,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    selectedResizeMode: PlayerResizeMode,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    audioTrackNames: List<String>,
    selectedAudioTrackIndex: Int,
    showAudioTracks: Boolean,
    onAudioTrackSelected: (Int) -> Unit,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    showSubtitleTracks: Boolean,
    onSubtitleTrackSelected: (Int) -> Unit,
    subtitleStyle: PlayerSubtitleStyleSettings,
    onSubtitleStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
    showVolume: Boolean,
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    showStabilization: Boolean,
    stabilizationEnabled: Boolean,
    onStabilizationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            modifier = Modifier
                .fillMaxHeight()
                // В портрете — почти вся ширина, в ландшафте — 70% экрана справа.
                .then(if (isLandscape) Modifier else Modifier.widthIn(max = MobilePlayerSidePanelMaxWidth))
                .fillMaxWidth(
                    if (isLandscape) MobilePlayerSidePanelLandscapeFraction else MobilePlayerSidePanelPortraitFraction,
                )
                // Панель сама глотает тапы, чтобы они не закрывали её через скрим под ней.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            LazyColumn(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical),
                ),
                contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.player_side_panel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                item(key = "playback") {
                    MobilePlayerPlaybackSection(
                        qualities = qualities,
                        selectedQuality = selectedQuality,
                        onQualitySelected = onQualitySelected,
                        selectedSpeed = selectedSpeed,
                        onSpeedSelected = onSpeedSelected,
                        selectedResizeMode = selectedResizeMode,
                        onResizeModeSelected = onResizeModeSelected,
                    )
                }
                if (showAudioTracks) {
                    item(key = "alloha_audio") {
                        MobilePlayerAudioTrackSection(
                            trackNames = audioTrackNames,
                            selectedIndex = selectedAudioTrackIndex,
                            onTrackSelected = onAudioTrackSelected,
                        )
                    }
                }
                if (showSubtitleTracks) {
                    item(key = "subtitle_tracks") {
                        MobilePlayerSubtitleTrackSection(
                            trackNames = subtitleTrackNames,
                            selectedIndex = selectedSubtitleTrackIndex,
                            onTrackSelected = onSubtitleTrackSelected,
                        )
                    }
                }
                item(key = "subtitle_style") {
                    MobilePlayerSubtitleStyleSection(
                        style = subtitleStyle,
                        onStyleChange = onSubtitleStyleChange,
                    )
                }
                if (showVolume || showStabilization) {
                    item(key = "volume") {
                        MobilePlayerVolumeSection(
                            showVolume = showVolume,
                            volumePercent = volumePercent,
                            onVolumeChange = onVolumeChange,
                            showStabilization = showStabilization,
                            stabilizationEnabled = stabilizationEnabled,
                            onStabilizationChange = onStabilizationChange,
                        )
                    }
                }
            }
        }
    }
}

/** Предел ширины в портрете — для планшетов; на телефоне панель упирается в долю экрана. */
private val MobilePlayerSidePanelMaxWidth = 560.dp
private const val MobilePlayerSidePanelPortraitFraction = 0.92f
private const val MobilePlayerSidePanelLandscapeFraction = 0.7f
