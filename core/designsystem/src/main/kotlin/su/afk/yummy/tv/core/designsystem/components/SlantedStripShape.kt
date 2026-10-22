package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Параллелограмм со скосом «/»: верхняя кромка сдвинута вправо на [slant].
 * У крайних полос внешний край остаётся вертикальным ([flatLeft], [flatRight]), чтобы коллаж
 * целиком заполнял прямоугольную карточку.
 */
class SlantedStripShape(
    private val slant: Dp,
    private val flatLeft: Boolean = false,
    private val flatRight: Boolean = false,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val shift = with(density) { slant.toPx() }
        val path = Path().apply {
            moveTo(if (flatLeft) 0f else shift, 0f)
            lineTo(size.width, 0f)
            lineTo(if (flatRight) size.width else size.width - shift, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
