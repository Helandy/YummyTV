package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.TimelineNodeBadge
import su.afk.yummy.tv.core.model.anime.AnimeTypeKind
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Нода таймлайна над карточкой франшизы: подпись (год или «ВЫ ЗДЕСЬ»), кружок с номером и отрезок линии
 * слева/справа. Линия сплошная до текущего тайтла, дальше пунктир; продолжается на половину [gap] в обе стороны,
 * чтобы соседние ноды сливались в одну линию.
 */
@Composable
internal fun UniverseTimelineNode(
    index: Int,
    currentIndex: Int,
    count: Int,
    year: Int?,
    typeKind: AnimeTypeKind?,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    val isCurrent = index == currentIndex
    val primary = MaterialTheme.colorScheme.primary
    val passed = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val upcoming = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isCurrent) stringResource(R.string.details_universe_you_are_here) else year?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.height(LabelHeight),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NodeSize)
                .drawTimelineLine(index, currentIndex, count, gap, primary, passed, upcoming),
            contentAlignment = Alignment.Center,
        ) {
            TimelineNodeBadge(
                number = index + 1,
                typeKind = typeKind,
                current = isCurrent,
                size = NodeSize,
                textStyle = MaterialTheme.typography.labelSmall,
            )
        }
        Box(modifier = Modifier.height(NodeBottomSpacing))
    }
}

private fun Modifier.drawTimelineLine(
    index: Int,
    currentIndex: Int,
    count: Int,
    gap: Dp,
    primary: Color,
    passed: Color,
    upcoming: Color,
): Modifier = drawBehind {
    val half = gap.toPx() / 2f
    val y = size.height / 2f
    val cx = size.width / 2f
    if (index > 0) {
        drawSegment(index - 1, currentIndex, Offset(-half, y), Offset(cx, y), primary, passed, upcoming)
    }
    if (index < count - 1) {
        drawSegment(index, currentIndex, Offset(cx, y), Offset(size.width + half, y), primary, passed, upcoming)
    }
}

/** Отрезок между нодами `segment` и `segment + 1`: до текущего приглушён, к текущему яркий, после — пунктир. */
internal fun DrawScope.drawSegment(
    segment: Int,
    currentIndex: Int,
    start: Offset,
    end: Offset,
    primary: Color,
    passed: Color,
    upcoming: Color,
) {
    val width = LineWidth.toPx()
    when {
        segment + 1 < currentIndex -> drawLine(passed, start, end, strokeWidth = width)
        segment + 1 == currentIndex -> drawLine(primary, start, end, strokeWidth = width)
        else -> drawLine(
            color = upcoming,
            start = start,
            end = end,
            strokeWidth = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashLength.toPx())),
        )
    }
}

private val LabelHeight = 20.dp
private val NodeSize = 24.dp
private val NodeBottomSpacing = 10.dp
private val LineWidth = 2.dp
private val DashLength = 4.dp
