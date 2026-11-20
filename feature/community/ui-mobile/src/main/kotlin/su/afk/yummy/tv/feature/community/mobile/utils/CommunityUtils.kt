package su.afk.yummy.tv.feature.community.mobile.utils

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.community.model.CommunitySection
import su.afk.yummy.tv.feature.community.presentation.R

/** Иконка плитки раздела. */
internal fun CommunitySection.icon(): ImageVector = when (this) {
    CommunitySection.REVIEWS -> Icons.Filled.RateReview
    CommunitySection.COLLECTIONS -> Icons.Filled.CollectionsBookmark
    CommunitySection.SCHEDULE -> Icons.Filled.DateRange
    CommunitySection.CHAT -> Icons.Filled.Forum
    CommunitySection.FRIENDS -> Icons.Filled.PersonAdd
    CommunitySection.USERS -> Icons.Filled.Groups
    CommunitySection.ABOUT -> Icons.Filled.Info
}

/** Подпись плитки раздела. */
@StringRes
internal fun CommunitySection.titleRes(): Int = when (this) {
    CommunitySection.REVIEWS -> R.string.community_section_reviews
    CommunitySection.COLLECTIONS -> R.string.community_section_collections
    CommunitySection.SCHEDULE -> R.string.community_section_schedule
    CommunitySection.CHAT -> R.string.community_section_chat
    CommunitySection.FRIENDS -> R.string.community_section_friends
    CommunitySection.USERS -> R.string.community_section_users
    CommunitySection.ABOUT -> R.string.community_section_about
}
