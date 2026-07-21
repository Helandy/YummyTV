package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTitleListCard
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.formatUserListDate
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileSurfacePanel

@Composable
internal fun UserAnimeListRow(item: UserAnimeListItem, onClick: () -> Unit) {
    val rating = item.userRating
        ?.takeIf { it in 1..10 }
        ?.toDouble()
    val addedDate = item.updatedAtSeconds
        ?.formatUserListDate()
        ?.takeIf { it.isNotBlank() }

    MobileTitleListCard(
        title = item.title.ifBlank { stringResource(R.string.user_profile_untitled) },
        posterUrl = item.posterUrl,
        dateText = addedDate,
        rating = rating,
        modifier = Modifier
            .padding(horizontal = 16.dp),
        onClick = onClick,
    )
}

@Composable
internal fun UserCollectionRow(item: AnimeCollectionSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.posterUrl,
        title = item.title,
        subtitle = item.description,
        onClick = onClick,
    )
}

@Composable
internal fun UserPostRow(item: UserPostSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.previewImageUrl,
        title = item.title,
        subtitle = item.contentPreview.ifBlank { item.categoryTitle },
        onClick = onClick.takeIf { item.id > 0 },
    )
}

@Composable
internal fun UserReviewRow(item: UserReviewSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.animePosterUrl,
        title = item.animeTitle.ifBlank { stringResource(R.string.user_profile_review) },
        subtitle = item.textPreview,
        onClick = onClick.takeIf { item.id > 0 },
    )
}

@Composable
internal fun UserFriendRow(item: UserFriend, onClick: () -> Unit) {
    AccountMobileSurfacePanel(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.status.isNotBlank()) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun UserProfileMediaRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    AccountMobileSurfacePanel(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(clickModifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title.ifBlank { stringResource(R.string.user_profile_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun UserProfileMessage(
    text: String,
    action: String,
    onAction: () -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(onClick = onAction, label = { Text(action) })
        }
    }
}
