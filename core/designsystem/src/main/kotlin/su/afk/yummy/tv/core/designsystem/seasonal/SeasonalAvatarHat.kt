package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/**
 * Шапка Деда Мороза (Новый год) на круглой аватарке. Ставить до `clip`, иначе шапку обрежет круг; размер берётся
 * по меньшей стороне. Хэллоуинский декор рисует [SeasonalAvatarFrame].
 */
@Composable
fun Modifier.seasonalAvatarHat(): Modifier = when (rememberSeasonalTheme()) {
    SeasonalTheme.NEW_YEAR -> drawWithContent {
        drawContent()
        drawSantaHat(minOf(size.width, size.height))
    }
    SeasonalTheme.HALLOWEEN, SeasonalTheme.NONE -> this
}
