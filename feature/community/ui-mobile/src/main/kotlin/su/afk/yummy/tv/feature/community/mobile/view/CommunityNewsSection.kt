package su.afk.yummy.tv.feature.community.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileNewsCard
import su.afk.yummy.tv.core.utils.formatting.formatFeedDateTime
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.posts.model.PostSummary

/** Горизонтальная лента последних новостей с заголовком и переходом ко всем. */
@Composable
internal fun CommunityNewsSection(
    title: String,
    allLabel: String,
    news: List<PostSummary>,
    onNewsSelected: (PostSummary) -> Unit,
    onAllSelected: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            trailingActionLabel = allLabel,
            onTrailingActionClick = onAllSelected,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(news, key = { lazyKey("communitynews", it.id) }) { post ->
                MobileNewsCard(
                    title = post.title,
                    imageUrl = post.previewImageUrl,
                    category = post.category.title,
                    preview = post.contentPreview,
                    meta = "${post.author.nickname} · ${post.createdAt.formatFeedDateTime()}",
                    onClick = { onNewsSelected(post) },
                    modifier = Modifier.width(300.dp),
                )
            }
        }
    }
}
