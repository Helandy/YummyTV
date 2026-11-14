package su.afk.yummy.tv.feature.player.mobile.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.baseScreen.HideSheetWindowSystemBars
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMenuActions
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMenuModel
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsPage
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Единое меню настроек плеера в стиле YouTube: одна шторка с главным списком (качество, скорость,
 * озвучка и плеер, дорожки, блокировка, «Ещё») и вложенными страницами. Применяет всё сразу -
 * настройки общие с экраном «Настройки». Шторка «Озвучка и плеер» (большие списки) открывается
 * отдельно через [MobilePlayerSettingsMenuActions.onDubbingAndPlayer].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsMenuSheet(
    model: MobilePlayerSettingsMenuModel,
    actions: MobilePlayerSettingsMenuActions,
    onDismiss: () -> Unit,
    /** Страница, на которой меню открывается: нижняя панель ведёт сразу на «Скорость». */
    initialPage: MobilePlayerSettingsPage = MobilePlayerSettingsPage.Root,
) {
    var page by rememberSaveable { mutableStateOf(initialPage) }
    // Страницу, открытую сразу с нижней панели (скорость, качество), «назад» закрывает целиком:
    // иначе стрелка раскрывала бы главное меню, которого пользователь не открывал.
    val goBack = {
        if (initialPage != MobilePlayerSettingsPage.Root && page == initialPage) {
            onDismiss()
        } else {
            page = MobilePlayerSettingsPage.Root
        }
    }
    BaseBottomSheet(onDismissRequest = onDismiss, scrollableContent = true, compactDragHandle = true) {
        HideSheetWindowSystemBars()
        // Назад на вложенной странице ведёт к главному списку, а не закрывает шторку. Обработчик
        // обязан жить внутри диалога шторки: снаружи его перебивает собственный back самой шторки.
        BackHandler(enabled = page != MobilePlayerSettingsPage.Root, onBack = goBack)
        when (page) {
            MobilePlayerSettingsPage.Root -> MobilePlayerMenuRoot(
                model = model,
                actions = actions,
                onNavigate = { page = it },
                onDismiss = onDismiss,
            )

            MobilePlayerSettingsPage.Quality -> MobilePlayerQualityPage(model, actions, goBack)
            MobilePlayerSettingsPage.Speed -> MobilePlayerSpeedPage(
                speed = model.speed,
                onSpeedSelected = actions.onSpeedSelected,
                onBack = goBack,
            )

            MobilePlayerSettingsPage.Subtitles -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MobilePlayerMenuPageHeader(
                    title = stringResource(R.string.player_menu_subtitles),
                    onBack = goBack,
                )
                if (model.showSubtitleTracks) {
                    MobilePlayerSubtitleTrackSection(
                        trackNames = model.subtitleTrackNames,
                        selectedIndex = model.selectedSubtitleTrackIndex,
                        onTrackSelected = actions.onSubtitleTrackSelected,
                    )
                }
                MobilePlayerSubtitleStyleSection(
                    style = model.subtitleStyle,
                    onStyleChange = actions.onSubtitleStyleChange,
                )
            }

            MobilePlayerSettingsPage.AudioTrack -> Column(modifier = Modifier.fillMaxWidth()) {
                MobilePlayerMenuPageHeader(
                    title = stringResource(R.string.player_menu_audio_track),
                    onBack = goBack,
                )
                MobilePlayerAudioTrackSection(
                    trackNames = model.audioTrackNames,
                    selectedIndex = model.selectedAudioTrackIndex,
                    onTrackSelected = actions.onAudioTrackSelected,
                )
            }

            MobilePlayerSettingsPage.SleepTimer -> MobilePlayerSleepTimerPage(
                selectedMinutes = model.sleepTimerMinutes,
                onSelected = actions.onSleepTimerSelected,
                onBack = goBack,
            )

            MobilePlayerSettingsPage.Resize -> Column(modifier = Modifier.fillMaxWidth()) {
                MobilePlayerMenuPageHeader(
                    title = stringResource(R.string.player_resize_title),
                    onBack = goBack,
                )
                MobilePlayerScaleSelector(
                    selectedMode = model.resizeMode,
                    onModeSelected = actions.onResizeModeSelected,
                )
            }

            MobilePlayerSettingsPage.CastAndPip -> MobilePlayerCastPipPage(
                showPictureInPicture = model.showPictureInPicture,
                showCast = model.showCast,
                onPictureInPicture = {
                    onDismiss()
                    actions.onPictureInPicture()
                },
                onBack = goBack,
            )

            MobilePlayerSettingsPage.Display -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MobilePlayerMenuPageHeader(
                    title = stringResource(R.string.player_menu_display),
                    onBack = goBack,
                )
                MobilePlayerMenuRow(
                    icon = Icons.Filled.BrightnessMedium,
                    label = stringResource(R.string.player_brightness_auto_title),
                    subtitle = stringResource(R.string.player_brightness_auto_hint),
                    trailing = MobilePlayerMenuRowTrailing.Toggle(model.autoBrightness),
                    onClick = { actions.onAutoBrightnessChange(!model.autoBrightness) },
                )
                if (model.showVolume) {
                    // Стабилизация - отдельной строкой ниже, секция тут только про независимую громкость.
                    MobilePlayerVolumeSection(
                        showVolume = true,
                        volumePercent = model.volumePercent,
                        onVolumeChange = actions.onVolumeChange,
                        showStabilization = false,
                        stabilizationEnabled = model.stabilizationEnabled,
                        onStabilizationChange = actions.onStabilizationChange,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                if (model.showStabilization) {
                    MobilePlayerMenuRow(
                        icon = Icons.Filled.GraphicEq,
                        label = stringResource(R.string.player_volume_stabilization_title),
                        trailing = MobilePlayerMenuRowTrailing.Toggle(model.stabilizationEnabled),
                        onClick = { actions.onStabilizationChange(!model.stabilizationEnabled) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MobilePlayerMenuRoot(
    model: MobilePlayerSettingsMenuModel,
    actions: MobilePlayerSettingsMenuActions,
    onNavigate: (MobilePlayerSettingsPage) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Качество и скорость вынесены кнопками на нижнюю панель плеера - в меню их нет (страницы те же).
        if (model.showAudioTracks) {
            MobilePlayerMenuRow(
                icon = Icons.Filled.Audiotrack,
                label = stringResource(R.string.player_menu_audio_track),
                trailing = MobilePlayerMenuRowTrailing.Navigate(
                    model.audioTrackNames.getOrNull(model.selectedAudioTrackIndex),
                ),
                onClick = { onNavigate(MobilePlayerSettingsPage.AudioTrack) },
            )
        }
        // Выбор субтитров есть только у Alloha (см. PlayerTrackMenu.showSubtitleChoice): для остальных пункт не нужен.
        if (model.showSubtitleTracks) {
            MobilePlayerMenuRow(
                icon = Icons.Filled.Subtitles,
                label = stringResource(R.string.player_menu_subtitles),
                trailing = MobilePlayerMenuRowTrailing.Navigate(
                    if (model.showSubtitleTracks) {
                        model.subtitleTrackNames.getOrNull(model.selectedSubtitleTrackIndex)
                    } else {
                        null
                    },
                ),
                onClick = { onNavigate(MobilePlayerSettingsPage.Subtitles) },
            )
        }
        MobilePlayerMenuRow(
            icon = Icons.Filled.Bedtime,
            label = stringResource(R.string.player_sleep_title),
            trailing = MobilePlayerMenuRowTrailing.Navigate(
                if (model.sleepTimerMinutes > 0) {
                    sleepTimerLabel(model.sleepTimerMinutes)
                } else {
                    stringResource(R.string.player_sleep_off)
                },
            ),
            onClick = { onNavigate(MobilePlayerSettingsPage.SleepTimer) },
        )
        MobilePlayerMenuRow(
            icon = Icons.Filled.AspectRatio,
            label = stringResource(R.string.player_resize_title),
            trailing = MobilePlayerMenuRowTrailing.Navigate(),
            onClick = { onNavigate(MobilePlayerSettingsPage.Resize) },
        )
        MobilePlayerMenuRow(
            icon = Icons.Filled.BrightnessMedium,
            label = stringResource(R.string.player_menu_display),
            trailing = MobilePlayerMenuRowTrailing.Navigate(),
            onClick = { onNavigate(MobilePlayerSettingsPage.Display) },
        )
        if (model.showPictureInPicture || model.showCast) {
            MobilePlayerMenuRow(
                icon = Icons.Filled.Cast,
                label = stringResource(R.string.player_menu_cast_pip),
                trailing = MobilePlayerMenuRowTrailing.Navigate(),
                onClick = { onNavigate(MobilePlayerSettingsPage.CastAndPip) },
            )
        }
        if (model.showDetails) {
            MobilePlayerMenuRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.player_mobile_details),
                trailing = MobilePlayerMenuRowTrailing.None,
                onClick = {
                    onDismiss()
                    actions.onDetails()
                },
            )
        }
    }
}
