package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/**
 * Украшение шторок и диалогов. Новый год — снежная шапка по верхней кромке. Хэллоуин: паутина в обоих верхних углах и паук на нити из
 * правой. Рисуется поверх содержимого, но только в углах.
 */
@Composable
fun Modifier.seasonalPanelDecor(): Modifier {
    val theme = rememberSeasonalTheme()
    val phase = rememberSpiderPhase()
    if (theme == SeasonalTheme.NEW_YEAR) {
        return drawWithContent {
            drawContent()
            // Отступ от краёв, чтобы шапка не торчала за скруглёнными углами шторки.
            drawSnowCap(capHeight = 4.dp.toPx(), inset = 24.dp.toPx())
        }
    }
    if (theme != SeasonalTheme.HALLOWEEN) return this
    return drawWithContent {
        drawContent()
        val web = minOf(size.width * 0.12f, 40.dp.toPx())
        // Слева заголовок почти у края, поэтому паутина там меньше, чем справа, где ничего нет.
        drawCornerWeb(Offset.Zero, 1f, web * 0.4f)
        drawCornerWeb(Offset(size.width, 0f), -1f, web)
        drawSpiderOnThread(size.width - 18.dp.toPx(), 40.dp.toPx(), phase.value)
    }
}
