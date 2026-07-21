package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.domain.account.model.UserPostSummary

@Composable
internal fun UserPostRow(item: UserPostSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.previewImageUrl,
        title = item.title,
        subtitle = item.contentPreview.ifBlank { item.categoryTitle },
        onClick = onClick.takeIf { item.id > 0 },
    )
}
