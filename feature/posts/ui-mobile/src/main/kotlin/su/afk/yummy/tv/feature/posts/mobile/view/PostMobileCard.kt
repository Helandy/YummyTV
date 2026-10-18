package su.afk.yummy.tv.feature.posts.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileNewsCard
import su.afk.yummy.tv.core.utils.formatting.formatFeedDateTime
import su.afk.yummy.tv.domain.posts.model.PostSummary

@Composable
fun PostMobileCard(post: PostSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MobileNewsCard(
        title = post.title,
        imageUrl = post.previewImageUrl,
        category = post.category.title,
        preview = post.contentPreview,
        meta = "${post.author.nickname} · ${post.createdAt.formatFeedDateTime()}",
        onClick = onClick,
        modifier = modifier,
    )
}
