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
private val SnowCapLastDigits = 4..7

/**
 * Праздничная накладка на постер карточки: не на каждой, а по последней цифре [itemId]: на Хэллоуин — паутина в углу при 1–3, на Новый год —
 * снежная шапка при 4–7.
 */
@Composable
fun Modifier.seasonalCardDecor(itemId: Int): Modifier {
    val theme = rememberSeasonalTheme()
    val lastDigit = abs(itemId % 10)
    return when (theme) {
        SeasonalTheme.NONE -> this
        SeasonalTheme.HALLOWEEN -> if (lastDigit !in CobwebLastDigits) this else drawWithContent {
            drawContent()
            drawCornerWeb(Offset.Zero, 1f, size.width * CobwebToCardWidth)
        }
        SeasonalTheme.NEW_YEAR -> if (lastDigit !in SnowCapLastDigits) this else drawWithContent {
            drawContent()
            drawSnowCap(capHeight = 4.dp.toPx())
        }
    }
}
