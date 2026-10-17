package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.model.TvPlayerSettingsPage

/**
 * Боковое меню настроек ТВ-плеера справа, в два уровня, как в системных плеерах Android TV: пункт
 * показывает текущее значение, OK открывает список вариантов с галочкой, выбор или Назад/← —
 * возврат к меню. Громкость меняется ← / → прямо в пункте, стабилизация — по OK.
 */
@Composable
internal fun TvPlayerSettingsPanel(
    visible: Boolean,
    entryFocusRequester: FocusRequester,
    speeds: List<Float>,
    activeSpeed: Float,
    onSpeedSelected: (index: Int) -> Unit,
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onSubtitleTrackSelected: (index: Int) -> Unit,
    subtitleStyle: PlayerSubtitleStyleSettings,
    onSubtitleStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
    showVolume: Boolean,
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    showStabilization: Boolean,
    stabilizationEnabled: Boolean,
    onStabilizationChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
    ) {
        // Состояние живёт внутри AnimatedVisibility: при каждом открытии меню начинается с корня.
        var page by remember { mutableStateOf<TvPlayerSettingsPage?>(null) }
        var returnPage by remember { mutableStateOf<TvPlayerSettingsPage?>(null) }
        val rowFocus = remember { TvPlayerSettingsPage.entries.associateWith { FocusRequester() } }
        val optionFocus = remember { FocusRequester() }

        fun openPage(target: TvPlayerSettingsPage) {
            returnPage = target
            page = target
        }

        // Вход в подменю — фокус на текущий вариант; возврат — на пункт, из которого заходили.
        LaunchedEffect(page) {
            val current = page
            if (current != null) {
                requestFocusUntilTimeout(optionFocus)
            } else {
                returnPage?.let { requestFocusUntilTimeout(rowFocus.getValue(it)) }
            }
        }

        // Объявлен позже BackHandler'а плеера, поэтому в подменю Назад сначала ведёт к корню.
        BackHandler(enabled = page != null) { page = null }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp)
                .background(
                    YummySemanticColors.PanelScrim,
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                )
                .focusProperties {
                    onExit = {
                        // Гасим только D-pad-выходы: программный возврат фокуса на кнопку после
                        // закрытия тоже проходит через onExit и отменять его нельзя.
                        when (requestedFocusDirection) {
                            FocusDirection.Left -> if (page != null) {
                                page = null
                                cancelFocusChange()
                            } else {
                                onClose()
                            }

                            FocusDirection.Up, FocusDirection.Down, FocusDirection.Right -> cancelFocusChange()
                            else -> Unit
                        }
                    }
                }
                .focusGroup()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val current = page) {
                null -> TvPlayerSettingsRoot(
                    entryFocusRequester = entryFocusRequester,
                    rowFocus = rowFocus,
                    activeSpeed = activeSpeed,
                    resizeMode = resizeMode,
                    zoomLevel = zoomLevel,
                    subtitleTrackNames = subtitleTrackNames,
                    selectedSubtitleTrackIndex = selectedSubtitleTrackIndex,
                    subtitleStyle = subtitleStyle,
                    showVolume = showVolume,
                    volumePercent = volumePercent,
                    onVolumeChange = onVolumeChange,
                    showStabilization = showStabilization,
                    stabilizationEnabled = stabilizationEnabled,
                    onStabilizationChange = onStabilizationChange,
                    onOpenPage = ::openPage,
                )

                else -> TvPlayerSettingsOptionsPage(
                    page = current,
                    optionFocus = optionFocus,
                    speeds = speeds,
                    activeSpeed = activeSpeed,
                    onSpeedSelected = onSpeedSelected,
                    resizeMode = resizeMode,
                    zoomLevel = zoomLevel,
                    onResizeModeSelected = onResizeModeSelected,
                    onZoomLevelSelected = onZoomLevelSelected,
                    subtitleTrackNames = subtitleTrackNames,
                    selectedSubtitleTrackIndex = selectedSubtitleTrackIndex,
                    onSubtitleTrackSelected = onSubtitleTrackSelected,
                    subtitleStyle = subtitleStyle,
                    onSubtitleStyleChange = onSubtitleStyleChange,
                    onDone = { page = null },
                )
            }
        }
    }
}
