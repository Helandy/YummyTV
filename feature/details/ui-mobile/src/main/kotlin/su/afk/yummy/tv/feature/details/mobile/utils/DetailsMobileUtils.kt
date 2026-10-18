package su.afk.yummy.tv.feature.details.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.formatting.toCompactDecimal
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.details_count_millions,
        (this / 1_000_000f).toCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        R.string.details_count_thousands,
        (this / 1_000f).toCompactDecimal(),
    )

    else -> toString()
}

/**
 * Обрезает высоту по базовой линии последней строки: низ цифр встаёт ровно на нижний край,
 * без запаса шрифта под выносные элементы (он рисуется за границей).
 */
internal fun Modifier.trimBelowLastBaseline(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val baseline = placeable[LastBaseline]
    val height = if (baseline == AlignmentLine.Unspecified) placeable.height else baseline
    layout(placeable.width, height) { placeable.place(0, 0) }
}
