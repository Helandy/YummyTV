package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileSortDropdown
import su.afk.yummy.tv.core.utils.formatting.toCompactCount
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.presentation.R
import su.afk.yummy.tv.feature.comments.utils.dropdownLabelRes

/** Порядок сортировок комментариев в выпадающем меню и страниц пейджера (Популярные/Новые/Старые). */
internal val commentSortOrder = listOf(CommentSort.BEST, CommentSort.NEW, CommentSort.OLD)

/** Высота шапки вместе с внешними отступами: список резервирует её сверху, чтобы шапка не перекрывала первый элемент. */
internal val CommentSortRowHeight = 56.dp

@Composable
internal fun CommentSortRow(
    selected: CommentSort,
    totalCount: Int?,
    onSelected: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (totalCount != null && totalCount > 0) {
            Text(
                text = stringResource(R.string.comments_total, totalCount.toCompactCount()).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        } else {
            Spacer(Modifier.width(1.dp))
        }
        MobileSortDropdown(
            items = commentSortOrder,
            selected = selected,
            label = { stringResource(it.dropdownLabelRes()) },
            onSelected = onSelected,
        )
    }
}
