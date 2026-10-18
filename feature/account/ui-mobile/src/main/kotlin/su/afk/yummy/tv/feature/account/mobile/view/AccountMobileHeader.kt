package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileHeader(
    state: AccountState.State,
    profileSummary: UserProfileSummary?,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nickname = profileSummary?.nickname?.ifBlank { state.nickname } ?: state.nickname
    AccountMobileProfileHero(
        bannerUrl = profileSummary?.bannerUrl.orEmpty(),
        bannerWideUrl = profileSummary?.bannerWideUrl,
        avatarUrl = profileSummary?.avatarUrl ?: state.avatarUrl,
        nickname = nickname,
        modifier = modifier,
        topActions = {
            AccountMobileHeroActionButton(
                icon = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.account_edit_profile),
                onClick = onEditClick,
            )
            AccountMobileHeroActionButton(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.account_logout),
                onClick = onLogoutClick,
            )
        },
    ) {
        AccountMobileProfileIdentity(
            nickname = nickname.ifBlank { stringResource(R.string.account_unknown_user) },
            summary = profileSummary,
        )
    }
}
