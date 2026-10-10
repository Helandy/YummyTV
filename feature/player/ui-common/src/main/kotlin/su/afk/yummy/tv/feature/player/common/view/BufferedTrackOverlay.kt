package su.afk.yummy.tv.feature.player.common.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Рисует поверх дорожки слайдера буфер (за проигранной частью) и метку опенинга.
 * Доли — `0f..1f` от длительности; в RTL-раскладке направление зеркалится.
 */
fun Modifier.bufferedTrackOverlay(
    activeProgress: Float,
    bufferedProgress: Float,
    color: Color,
    openingRange: ClosedFloatingPointRange<Float>?,
    openingColor: Color,
): Modifier =
    drawWithContent {
        drawContent()

        val trackY = size.height / 2f
        val strokeWidth = 4.dp.toPx()

        // Метка опенинга: рисуем поверх дорожки, чтобы отрезок был виден и под проигранной частью.
        if (openingRange != null) {
            val oStartX: Float
            val oEndX: Float
            if (layoutDirection == LayoutDirection.Rtl) {
                oStartX = (size.width * (1f - openingRange.start)).coerceIn(0f, size.width)
                oEndX = (size.width * (1f - openingRange.endInclusive)).coerceIn(0f, size.width)
            } else {
                oStartX = (size.width * openingRange.start).coerceIn(0f, size.width)
                oEndX = (size.width * openingRange.endInclusive).coerceIn(0f, size.width)
            }
            if (oStartX != oEndX) {
                drawLine(
                    color = openingColor,
                    start = Offset(oStartX, trackY),
                    end = Offset(oEndX, trackY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        if (bufferedProgress <= activeProgress) return@drawWithContent

        val gapPx = 6.dp.toPx()
        val startX: Float
        val endX: Float
        if (layoutDirection == LayoutDirection.Rtl) {
            startX = (size.width * (1f - activeProgress) - gapPx).coerceIn(0f, size.width)
            endX = (size.width * (1f - bufferedProgress)).coerceIn(0f, size.width)
        } else {
            startX = (size.width * activeProgress + gapPx).coerceIn(0f, size.width)
            endX = (size.width * bufferedProgress).coerceIn(0f, size.width)
        }
        if (startX == endX) return@drawWithContent
        drawLine(
            color = color,
            start = Offset(startX, trackY),
            end = Offset(endX, trackY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
