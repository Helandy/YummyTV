package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.utils.libraryMobileTabs
import su.afk.yummy.tv.feature.library.mobile.utils.mobileTitle

@Composable
internal fun LibraryMobileTabs(
    selectedTab: LibraryTab,
    tabCounts: Map<LibraryTab, Int>,
    onSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        val selectedIndex = libraryMobileTabs.indexOf(selectedTab).coerceAtLeast(0)
        listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(libraryMobileTabs, key = { it.name }) { tab ->
                LibraryMobileTabChip(
                    title = tab.mobileTitle(),
                    count = tabCounts[tab] ?: 0,
                    color = tab.mobileTabColor(),
                    selected = tab == selectedTab,
                    onClick = { onSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun LibraryMobileTabChip(
    title: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) {
            color.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LibraryMobileTabCountBadge(count = count, color = color, selected = selected)
        }
    }
}

@Composable
private fun LibraryMobileTabCountBadge(count: Int, color: Color, selected: Boolean) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) {
            color.copy(alpha = 0.28f)
        } else {
            color.copy(alpha = 0.18f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .heightIn(min = 22.dp)
                .widthIn(min = 24.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun LibraryTab.mobileTabColor(): Color =
    when (this) {
        LibraryTab.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
        LibraryTab.HISTORY -> MaterialTheme.colorScheme.tertiary
        LibraryTab.WATCHING -> Color(0xFFFF6B6B)
        LibraryTab.PLANNED -> Color(0xFFA678E8)
        LibraryTab.COMPLETED -> Color(0xFF69D38B)
        LibraryTab.POSTPONED -> Color(0xFFFFC857)
        LibraryTab.DROPPED -> Color(0xFF9CA3AF)
        LibraryTab.FAVORITES -> Color(0xFFD86BFF)
    }
