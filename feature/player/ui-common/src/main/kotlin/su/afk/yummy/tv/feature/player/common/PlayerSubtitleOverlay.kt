package su.afk.yummy.tv.feature.player.common

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings

/**
 * Рендер текущих субтитров поверх видео. Player сам решает, есть ли активная text-дорожка
 * (см. [PlayerTrackSelectionState.selectText]) — здесь только отображение [Player.Listener.onCues]
 * и пользовательское оформление из [style]. Явные line/position реплик (в т.ч. \pos из
 * AllohaAssPositionFix) сбрасываем — иначе они перебивают [style.offset], заданный пользователем.
 * А одновременные реплики склеиваем в один блок ([mergeSimultaneousCues]): SubtitleView не разводит
 * cue по вертикали, и без склейки они рисовались бы поверх друг друга (issue #23).
 */
@Composable
fun PlayerSubtitleOverlay(
    player: Player?,
    style: PlayerSubtitleStyleSettings,
    modifier: Modifier = Modifier,
) {
    var cueGroup by remember { mutableStateOf<CueGroup?>(null) }

    DisposableEffect(player) {
        if (player == null) {
            onDispose {}
        } else {
            cueGroup = player.currentCues
            val listener = object : Player.Listener {
                override fun onCues(cues: CueGroup) {
                    cueGroup = cues
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            SubtitleView(context).apply {
                // Alloha отдаёт ASS/SSA со своими размерами и цветами — без этого пользовательские
                // настройки просто игнорировались бы встроенными стилями дорожки.
                setApplyEmbeddedStyles(false)
                setApplyEmbeddedFontSizes(false)
            }
        },
        update = { view ->
            view.setCues(cueGroup?.cues.orEmpty().mergeSimultaneousCues())
            view.setFractionalTextSize(
                SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (style.textSize / 100f)
            )
            view.setBottomPaddingFraction(style.offset / 100f)
            view.setStyle(
                CaptionStyleCompat(
                    style.textColor.argb,
                    style.background.argb,
                    Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    Color.BLACK,
                    null,
                )
            )
        },
    )
}
