package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileTitleListCard
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.account.mobile.userprofile.mapper.toTitleListCardUi
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

@Composable
internal fun UserAnimeListRow(
    item: UserAnimeListItem,
    filter: UserProfileState.ListFilter,
    showYear: Boolean,
    isOwnProfile: Boolean,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
) {
    val untitled = stringResource(R.string.user_profile_untitled)
    val posterQuality = LocalPosterQuality.current
    val card = remember(item, filter, untitled, posterQuality) {
        item.toTitleListCardUi(filter, untitled, posterQuality)
    }
    MobileTitleListCard(
        item = card,
        nowEpochSeconds = nowEpochSeconds,
        showYear = showYear,
        // В чужом профиле свои список и оценка — отдельной полосой, чтобы не путать с владельцем.
        showViewerMark = !isOwnProfile,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
