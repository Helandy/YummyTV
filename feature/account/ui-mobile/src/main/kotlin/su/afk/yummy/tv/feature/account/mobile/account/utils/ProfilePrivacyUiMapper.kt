package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun ProfileListPrivacy.label() = stringResource(
    when (this) {
        ProfileListPrivacy.PUBLIC -> R.string.profile_privacy_public
        ProfileListPrivacy.FRIENDS -> R.string.profile_privacy_friends
        ProfileListPrivacy.AUTHORIZED -> R.string.profile_privacy_authorized
        ProfileListPrivacy.PRIVATE -> R.string.profile_privacy_private
    }
)
