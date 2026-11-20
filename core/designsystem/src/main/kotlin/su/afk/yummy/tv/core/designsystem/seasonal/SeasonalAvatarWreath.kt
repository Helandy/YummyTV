package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/**
 * Новогодний венок (Новый год) по краю круглой аватарки. Ставить до `clip`, иначе венок обрежет круг; размер берётся
 * по меньшей стороне. Хэллоуинский декор рисует [SeasonalAvatarFrame].
 */
@Composable
fun Modifier.seasonalAvatarWreath(): Modifier = when (rememberSeasonalTheme()) {
    SeasonalTheme.NEW_YEAR -> drawWithContent {
        drawContent()
        drawNewYearWreath(minOf(size.width, size.height))
    }
    SeasonalTheme.HALLOWEEN, SeasonalTheme.NONE -> this
}
