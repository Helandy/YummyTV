package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.presentation.R

internal val DetailsDescriptionPadding = 12.dp

/** Начало описания: стоит рядом с постером, по OK раскрывает остаток. */
@Composable
internal fun DetailsDescriptionHead(
    text: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableClick(onClick = onToggle, shape = RoundedCornerShape(8.dp), focusedScale = 1.01f)
            .padding(DetailsDescriptionPadding),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

/** Остаток описания на всю ширину под постером (тоже фокусируется и раскрывает по OK); свёрнутый обрезается, раскрытый тянет хиро вниз. */
@Composable
internal fun DetailsDescriptionTail(
    text: String,
    expanded: Boolean,
    overflows: Boolean,
    onOverflowChanged: (Boolean) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableClick(onClick = onToggle, shape = RoundedCornerShape(8.dp), focusedScale = 1.01f)
            .padding(DetailsDescriptionPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_TAIL_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) onOverflowChanged(it.hasVisualOverflow) },
        )
        if (overflows || expanded) {
            Text(
                text = stringResource(
                    if (expanded) R.string.details_mobile_less else R.string.details_mobile_more,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val DESCRIPTION_TAIL_COLLAPSED_LINES = 2
