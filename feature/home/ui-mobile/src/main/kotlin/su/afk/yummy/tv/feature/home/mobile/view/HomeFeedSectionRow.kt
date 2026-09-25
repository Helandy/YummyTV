package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.lazy.KeepLazyListAtStartOnNewHead
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.mobile.utils.showMobileCardMetadata

@Composable
internal fun HomeFeedSectionRow(
    section: HomeFeedSection,
    onItemSelected: (HomeFeedItem) -> Unit,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onItemLongClick: ((HomeFeedItem) -> Unit)? = null,
) {
    val showCardMetadata = section.type.showMobileCardMetadata()
    val listState = rememberLazyListState()
    KeepLazyListAtStartOnNewHead(state = listState, headKey = section.items.firstOrNull()?.id)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = section.title,
            modifier = Modifier.padding(horizontal = 16.dp),
            trailingActionLabel = actionLabel,
            onTrailingActionClick = onActionClick,
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = { lazyKey("homeitem", it.id) }) { item ->
                Box(modifier = Modifier.testTag("anime_card")) {
                    HomeItemCard(
                        item = item,
                        showMetadata = showCardMetadata,
                        showYear = section.type == HomeFeedSectionType.RECOMMENDATIONS,
                        onClick = { onItemSelected(item) },
                        onLongClick = onItemLongClick?.let { { it(item) } },
                    )
                }
            }
        }
    }
}
