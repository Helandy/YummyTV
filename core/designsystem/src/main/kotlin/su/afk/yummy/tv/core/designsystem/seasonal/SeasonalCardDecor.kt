package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import kotlin.math.abs

private const val CobwebToCardWidth = 0.42f
private val CobwebLastDigits = 1..3

/**
 * Праздничная накладка на постер карточки: на Хэллоуин — паутина в углу, но не на каждой, а только
 * если последняя цифра [itemId] от 1 до 3 (примерно у трети); на Новый год — снежная шапка.
 */
@Composable
fun Modifier.seasonalCardDecor(itemId: Int): Modifier = when (rememberSeasonalTheme()) {
    SeasonalTheme.NONE -> this
    SeasonalTheme.HALLOWEEN -> when (abs(itemId % 10)) {
        in CobwebLastDigits -> drawWithContent {
            drawContent()
            drawCornerWeb(Offset.Zero, 1f, size.width * CobwebToCardWidth)
        }
        else -> this
    }
    SeasonalTheme.NEW_YEAR -> drawWithContent {
        drawContent()
        drawSnowCap(capHeight = 4.dp.toPx())
    }
}
