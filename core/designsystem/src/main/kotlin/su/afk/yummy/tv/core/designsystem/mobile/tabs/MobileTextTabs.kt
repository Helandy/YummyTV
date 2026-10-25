package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Текстовые вкладки с подчёркиванием выбранной. [scrollable] — для длинных списков
 * (категории), иначе вкладки делят ширину поровну (сортировки).
 *
 * Свой layout вместо material3 TabRow: его subcompose-индикатор падает в Compose
 * («LayoutNode not found in RectList») при уходе экрана с вкладками.
 */
@Composable
fun MobileTextTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    if (scrollable) {
        val listState = rememberLazyListState()
        LaunchedEffect(selectedIndex) {
            if (selectedIndex in titles.indices) listState.animateScrollToItem(selectedIndex)
        }
        LazyRow(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            itemsIndexed(titles) { index, title ->
                MobileTextTab(title, index == selectedIndex, { onSelected(index) })
            }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, title ->
                MobileTextTab(
                    title = title,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MobileTextTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        // IntrinsicSize.Max: в LazyRow ширина не ограничена, и fillMaxWidth у индикатора иначе дало бы 0.
        modifier = modifier
            .width(IntrinsicSize.Max)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = color,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                ),
        )
    }
}
