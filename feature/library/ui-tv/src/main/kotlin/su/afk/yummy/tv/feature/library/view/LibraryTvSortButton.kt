package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.tvLabel

/** Кнопка в ряду вкладок: показывает текущую сортировку и открывает её выбор. */
@Composable
internal fun LibraryTvSortButton(
    sort: LibrarySort,
    direction: LibrarySortDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val container = if (focused) Color.White else Color.White.copy(alpha = 0.10f)
    val content = if (focused) Color.Black else Color.White

    Row(
        modifier = modifier
            .background(container, shape)
            .tvFocusableClick(
                onClick = onClick,
                shape = shape,
                interactionSource = interactionSource,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.library_mobile_sort_label, sort.tvLabel()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = when (direction) {
                LibrarySortDirection.DESC -> Icons.Filled.KeyboardArrowDown
                LibrarySortDirection.ASC -> Icons.Filled.KeyboardArrowUp
            },
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
    }
}
