package su.afk.yummy.tv.feature.catalog.mobile.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.catalog.presentation.R

@Composable
internal fun CatalogMobileFilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.search_filters)
    BadgedBox(
        badge = {
            if (activeCount > 0) {
                Badge {
                    Text(activeCount.coerceAtMost(99).toString())
                }
            }
        },
        modifier = modifier,
    ) {
        // Заливка и акцентный цвет, чтобы кнопка выглядела нажимаемой, а не частью поля
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
        ) {
            CatalogMobileFilterIcon()
        }
    }
}

@Composable
private fun CatalogMobileFilterIcon(
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        val knobRadius = 2.75.dp.toPx()
        val startX = size.width * 0.18f
        val endX = size.width * 0.82f
        val firstY = size.height * 0.3f
        val secondY = size.height * 0.5f
        val thirdY = size.height * 0.7f

        drawLine(
            color = color,
            start = Offset(startX, firstY),
            end = Offset(endX, firstY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(startX, secondY),
            end = Offset(endX, secondY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(startX, thirdY),
            end = Offset(endX, thirdY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        drawCircle(color = color, radius = knobRadius, center = Offset(size.width * 0.34f, firstY))
        drawCircle(color = color, radius = knobRadius, center = Offset(size.width * 0.66f, secondY))
        drawCircle(color = color, radius = knobRadius, center = Offset(size.width * 0.46f, thirdY))
    }
}
