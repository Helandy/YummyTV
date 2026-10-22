package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/**
 * Накладка на главную кнопку: на Хэллоуин — паутина в углу, паук, изредка спускающийся на нити;
 * на Новый год — снежная шапка. Рисуется поверх кнопки.
 */
@Composable
fun Modifier.seasonalButtonDecor(): Modifier {
    val theme = rememberSeasonalTheme()
    val phase = rememberSpiderPhase()
    return when (theme) {
        SeasonalTheme.NONE -> this
        SeasonalTheme.NEW_YEAR -> drawWithContent {
            drawContent()
            drawSnowCap(capHeight = 4.dp.toPx(), inset = 10.dp.toPx())
        }
        SeasonalTheme.HALLOWEEN -> drawWithContent {
            drawContent()
            drawCornerWeb(Offset(size.width, 0f), -1f, minOf(size.height * 0.95f, 64.dp.toPx()))
            drawSpiderOnThread(size.width - 56.dp.toPx(), size.height * 0.62f, phase.value)
        }
    }
}
