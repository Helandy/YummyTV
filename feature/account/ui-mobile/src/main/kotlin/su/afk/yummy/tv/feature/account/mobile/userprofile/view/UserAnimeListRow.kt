package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTitleListCard
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.formatUserListDate

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
