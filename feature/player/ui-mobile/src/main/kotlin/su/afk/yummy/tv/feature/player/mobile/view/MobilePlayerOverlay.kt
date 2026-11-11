package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun MobilePlayerOverlay(
    visible: Boolean,
    progress: PlayerPlaybackProgressState,
    openingStartMs: Long?,
    openingEndMs: Long?,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    /** Кнопка качества слева: у источника без выбора качества её нет. */
    showQuality: Boolean,
    onQuality: () -> Unit,
    onSpeed: () -> Unit,
    onLock: () -> Unit,
    onDubbing: () -> Unit,
    onOrientation: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Кнопка серий в ряду управления и раскрывающаяся горизонтальная лента под ним. */
    episodesAvailable: Boolean = false,
    episodesExpanded: Boolean = false,
    /** Горизонтальная лента серий поверх видео (и в портрете, и в ландшафте). */
    episodesStripVisible: Boolean = false,
    onEpisodesExpandedChange: (Boolean) -> Unit = {},
    episodesStrip: @Composable () -> Unit = {},
) {
    if (!visible) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.90f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MobilePlayerProgressRow(
            displayTime = progress.displayTimeMs,
            duration = progress.duration,
            seekProgress = progress.progressFraction,
            bufferedProgress = progress.bufferedProgress,
            openingStartMs = openingStartMs,
            openingEndMs = openingEndMs,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished,
        )
        // Слева направо: серии, озвучка, качество, замок (центр), скорость, поворот, настройки.
        // null - кнопки нет (серий или выбора качества у источника может не быть).
        val episodesAction = if (episodesAvailable) {
            BottomAction(
                Icons.Filled.VideoLibrary,
                stringResource(R.string.player_episodes_list_title),
            ) { onEpisodesExpandedChange(!episodesExpanded) }
        } else {
            null
        }
        val dubbingAction = BottomAction(
            Icons.AutoMirrored.Filled.VolumeUp,
            stringResource(R.string.player_mobile_track_settings_title),
            onDubbing,
        )
        val qualityAction = if (showQuality) {
            BottomAction(Icons.Filled.HighQuality, stringResource(R.string.player_quality_title), onQuality)
        } else {
            null
        }
        val lockAction = BottomAction(Icons.Filled.Lock, stringResource(R.string.player_mobile_lock), onLock)
        val speedAction = BottomAction(Icons.Filled.Speed, stringResource(R.string.player_speed_title), onSpeed)
        val orientationAction = BottomAction(
            Icons.Filled.ScreenRotation,
            stringResource(R.string.player_mobile_orientation),
            onOrientation,
        )
        val settingsAction = BottomAction(
            Icons.Filled.Settings,
            stringResource(R.string.player_mobile_settings_title),
            onSettings,
        )
        val configuration = LocalConfiguration.current
        val compactPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
            configuration.screenWidthDp < COMPACT_WIDTH_DP
        if (compactPortrait) {
            // Узкий портрет: семь равных слотов делят всю ширину ряда, кнопки крупнее обычных.
            // Пустой слот (нет серий/качества) занимает место, чтобы замок остался строго по центру.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val buttonSize = ((maxWidth - BOTTOM_BUTTONS_MIN_GAP * (BOTTOM_SLOTS - 1)) / BOTTOM_SLOTS)
                    .coerceIn(BOTTOM_BUTTON_MIN_SIZE, BOTTOM_BUTTON_MAX_SIZE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        episodesAction,
                        dubbingAction,
                        qualityAction,
                        lockAction,
                        speedAction,
                        orientationAction,
                        settingsAction,
                    ).forEach { action ->
                        if (action != null) {
                            MobilePlayerActionButton(
                                icon = action.icon,
                                contentDescription = action.description,
                                onClick = action.onClick,
                                size = buttonSize,
                            )
                        } else {
                            Spacer(modifier = Modifier.size(buttonSize))
                        }
                    }
                }
            }
        } else {
            // Box, а не SpaceBetween: замок должен стоять ровно по центру, даже если группы слева и справа разной ширины.
            Box(modifier = Modifier.fillMaxWidth()) {
                MobilePlayerActionButton(
                    icon = lockAction.icon,
                    contentDescription = lockAction.description,
                    onClick = lockAction.onClick,
                    modifier = Modifier.align(Alignment.Center),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BOTTOM_BUTTONS_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    listOfNotNull(episodesAction, dubbingAction, qualityAction).forEach { action ->
                        MobilePlayerActionButton(action.icon, action.description, action.onClick)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BOTTOM_BUTTONS_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    listOf(speedAction, orientationAction, settingsAction).forEach { action ->
                        MobilePlayerActionButton(action.icon, action.description, action.onClick)
                    }
                }
            }
        }
        if (episodesAvailable) {
            AnimatedVisibility(
                visible = episodesStripVisible,
                enter = expandVertically(animationSpec = tween(EPISODES_STRIP_ANIMATION_MS)),
                exit = shrinkVertically(animationSpec = tween(EPISODES_STRIP_ANIMATION_MS)),
                modifier = Modifier.pointerInput(Unit) { collapseOnSwipeDown(onEpisodesExpandedChange) },
            ) {
                episodesStrip()
            }
        }
    }
}

private suspend fun PointerInputScope.collapseOnSwipeDown(onExpandedChange: (Boolean) -> Unit) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onVerticalDrag = { _, delta -> total += delta },
        onDragEnd = { if (total > SWIPE_THRESHOLD_PX) onExpandedChange(false) },
    )
}

/** Кнопка нижнего ряда: значок, подпись для TalkBack и действие. */
private class BottomAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
)

private val BOTTOM_BUTTONS_GAP = 6.dp

/** Ниже этой ширины (dp) в портрете кнопки нижнего ряда делят ряд поровну. */
private const val COMPACT_WIDTH_DP = 600
private const val BOTTOM_SLOTS = 7
private val BOTTOM_BUTTONS_MIN_GAP = 6.dp
private val BOTTOM_BUTTON_MIN_SIZE = 40.dp
private val BOTTOM_BUTTON_MAX_SIZE = 52.dp
private const val SWIPE_THRESHOLD_PX = 40f

/** Линейная анимация вместо дефолтной пружины: у пружины длинный хвост, и лента «доезжает» с лагом. */
private const val EPISODES_STRIP_ANIMATION_MS = 220
