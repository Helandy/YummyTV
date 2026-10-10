package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.tv.TvChip
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.presentation.R
import su.afk.yummy.tv.feature.comments.utils.labelRes

@Composable
internal fun CommentsHeader(
    selectedSort: CommentSort,
    initialFocusRequester: FocusRequester,
    onSortSelected: (CommentSort) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(CommentSort.NEW, CommentSort.BEST, CommentSort.OLD).forEachIndexed { index, sort ->
            TvChip(
                label = stringResource(sort.labelRes()),
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .tvFocusableClick(
                    onClick = onRefresh,
                    shape = CircleShape,
                    focusedScale = 1.06f,
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.comments_refresh),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
