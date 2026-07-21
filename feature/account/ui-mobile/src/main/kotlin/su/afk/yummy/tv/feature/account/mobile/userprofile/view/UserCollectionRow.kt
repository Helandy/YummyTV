package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

@Composable
internal fun UserCollectionRow(item: AnimeCollectionSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.posterUrl,
        title = item.title,
        subtitle = item.description,
        onClick = onClick,
    )
}
