package su.afk.yummy.tv.feature.player.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleEdge
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Границы и шаги стиля субтитров для боковой панели плеера — общие для мобилки и ТВ, совпадают с
 * клампами DataStore (размер 50..200, отступ 0..20).
 */
object PlayerSubtitleStyleOptions {
    val textSizeRange: IntRange = 50..200
    const val TEXT_SIZE_STEP: Int = 10
    val offsetRange: IntRange = 0..20
    const val OFFSET_STEP: Int = 1

    /** Все допустимые размеры — варианты для списка выбора на ТВ. */
    val textSizeValues: List<Int> = textSizeRange.step(TEXT_SIZE_STEP).toList()

    /** Все допустимые отступы — варианты для списка выбора на ТВ. */
    val offsetValues: List<Int> = offsetRange.step(OFFSET_STEP).toList()
}

@Composable
fun PlayerSubtitleTextColor.label(): String = stringResource(
    when (this) {
        PlayerSubtitleTextColor.WHITE -> R.string.player_subtitle_color_white
        PlayerSubtitleTextColor.YELLOW -> R.string.player_subtitle_color_yellow
        PlayerSubtitleTextColor.CYAN -> R.string.player_subtitle_color_cyan
        PlayerSubtitleTextColor.GREEN -> R.string.player_subtitle_color_green
        PlayerSubtitleTextColor.ORANGE -> R.string.player_subtitle_color_orange
        PlayerSubtitleTextColor.RED -> R.string.player_subtitle_color_red
        PlayerSubtitleTextColor.PINK -> R.string.player_subtitle_color_pink
        PlayerSubtitleTextColor.PURPLE -> R.string.player_subtitle_color_purple
        PlayerSubtitleTextColor.BLUE -> R.string.player_subtitle_color_blue
    },
)

@Composable
fun PlayerSubtitleBackground.label(): String = stringResource(
    when (this) {
        PlayerSubtitleBackground.NONE -> R.string.player_subtitle_background_none
        PlayerSubtitleBackground.TRANSLUCENT -> R.string.player_subtitle_background_translucent
        PlayerSubtitleBackground.SOLID -> R.string.player_subtitle_background_solid
    },
)

fun PlayerSubtitleBackground.icon(): ImageVector = when (this) {
    PlayerSubtitleBackground.NONE -> Icons.Filled.FormatColorReset
    PlayerSubtitleBackground.TRANSLUCENT -> Icons.Filled.Opacity
    PlayerSubtitleBackground.SOLID -> Icons.Filled.Square
}

@Composable
fun PlayerSubtitleEdge.label(): String = stringResource(
    when (this) {
        PlayerSubtitleEdge.NONE -> R.string.player_subtitle_edge_none
        PlayerSubtitleEdge.OUTLINE -> R.string.player_subtitle_edge_outline
        PlayerSubtitleEdge.SHADOW -> R.string.player_subtitle_edge_shadow
    },
)

fun PlayerSubtitleEdge.icon(): ImageVector = when (this) {
    PlayerSubtitleEdge.NONE -> Icons.Filled.Block
    PlayerSubtitleEdge.OUTLINE -> Icons.Filled.Title
    PlayerSubtitleEdge.SHADOW -> Icons.Filled.Layers
}
