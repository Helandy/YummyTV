package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.comments.model.CommentSort

/** Шапка сортировки поверх списка: появляется и прячется без изменения размеров списка. */
@Composable
internal fun CommentSortOverlay(
    visible: Boolean,
    selected: CommentSort,
    onSelected: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CommentSortRow(
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
