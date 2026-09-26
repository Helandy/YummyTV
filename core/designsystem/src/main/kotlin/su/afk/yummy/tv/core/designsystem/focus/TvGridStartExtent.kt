package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Расстояние от верха первого ряда грида до начала контента: верхний `contentPadding` плюс шапка
 * внутри грида (если есть) с межрядным отступом под ней. Шапка меряет себя сама через [measure].
 *
 * Передаётся в [tvWholeItemBringIntoView] карточкам первого ряда, чтобы вместе с рядом грид
 * показывал и шапку.
 */
@Stable
class TvGridStartExtent internal constructor(private val fixedPx: Float) {
    private var headerHeightPx by mutableIntStateOf(0)

    /** Вешается на корень item'а шапки грида. */
    val measure: Modifier = Modifier.onSizeChanged { headerHeightPx = it.height }

    internal fun px(): Float = fixedPx + headerHeightPx
}

/**
 * @param contentTopPadding верхний `contentPadding` грида.
 * @param rowSpacing межрядный отступ грида; учитывается только при [hasHeader].
 * @param hasHeader есть ли в гриде item-шапка над первым рядом (её надо померить [TvGridStartExtent.measure]).
 */
@Composable
fun rememberTvGridStartExtent(
    contentTopPadding: Dp,
    rowSpacing: Dp,
    hasHeader: Boolean = true,
): TvGridStartExtent {
    val fixedPx = with(LocalDensity.current) {
        contentTopPadding.toPx() + if (hasHeader) rowSpacing.toPx() else 0f
    }
    return remember(fixedPx) { TvGridStartExtent(fixedPx) }
}
