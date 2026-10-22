package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

/**
 * Шапка Деда Мороза (Новый год) или ведьмы (Хэллоуин) на круглой аватарке. Ставить до `clip`, иначе шапку обрежет
 * круг; размер берётся по меньшей стороне.
 */
@Composable
fun Modifier.seasonalAvatarHat(): Modifier = when (rememberSeasonalTheme()) {
    SeasonalTheme.NEW_YEAR -> drawWithContent {
        drawContent()
        drawSantaHat(minOf(size.width, size.height))
    }
    SeasonalTheme.HALLOWEEN -> drawWithContent {
        drawContent()
        drawWitchHat(minOf(size.width, size.height))
    }
    SeasonalTheme.NONE -> this
}
