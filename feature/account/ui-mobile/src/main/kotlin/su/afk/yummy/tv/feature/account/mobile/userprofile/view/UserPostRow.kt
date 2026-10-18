package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileNewsCard
import su.afk.yummy.tv.core.utils.formatting.decodeCommonHtmlEntities
import su.afk.yummy.tv.core.utils.formatting.formatFeedDateTime
import su.afk.yummy.tv.domain.account.model.UserPostSummary

/** Новость пользователя — та же карточка, что в ленте новостей; автор не нужен, он и есть владелец профиля. */
@Composable
internal fun UserPostRow(item: UserPostSummary, onClick: () -> Unit) {
    MobileNewsCard(
        title = item.title,
        imageUrl = item.previewImageUrl,
        category = item.categoryTitle,
        preview = item.contentPreview.decodeCommonHtmlEntities(),
        meta = item.createdAtSeconds.takeIf { it > 0L }?.formatFeedDateTime().orEmpty(),
        onClick = { if (item.id > 0) onClick() },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
