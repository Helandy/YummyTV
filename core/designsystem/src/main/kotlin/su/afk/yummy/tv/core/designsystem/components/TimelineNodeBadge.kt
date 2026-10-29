package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import su.afk.yummy.tv.core.model.anime.AnimeTypeKind

/**
 * Нода таймлайна франшизы с номером тайтла. Сериал, малометражный сериал и полнометражный фильм — круг,
 * все остальные форматы (спешл, OVA, ONA, короткометражка) — ромб.
 */
@Composable
fun TimelineNodeBadge(
    number: Int,
    typeKind: AnimeTypeKind?,
    current: Boolean,
    size: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(typeKind.timelineNodeShape())
                .background(if (current) colors.primary else colors.surfaceVariant),
        )
        Text(
            text = number.toString(),
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = if (current) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

private fun AnimeTypeKind?.timelineNodeShape(): Shape = when (this) {
    AnimeTypeKind.TV, AnimeTypeKind.SHORT_TV, AnimeTypeKind.MOVIE, null -> CircleShape
    AnimeTypeKind.SHORT_MOVIE, AnimeTypeKind.OVA, AnimeTypeKind.ONA, AnimeTypeKind.SPECIAL, AnimeTypeKind.OTHER -> DiamondShape
}

private object DiamondShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(
            Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width / 2f, size.height)
                lineTo(0f, size.height / 2f)
                close()
            },
        )
}
