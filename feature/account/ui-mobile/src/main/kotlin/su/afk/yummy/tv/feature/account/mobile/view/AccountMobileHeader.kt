package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R

/**
 * Шапка своего профиля: настройки и выход вверху справа поверх баннера, «Редактировать» внизу
 * справа в карточке.
 */
@Composable
internal fun AccountMobileHeader(
    state: AccountState.State,
    profileSummary: UserProfileSummary?,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    topExtra: Dp = 0.dp,
) {
    val nickname = profileSummary?.nickname?.ifBlank { state.nickname } ?: state.nickname
    AccountMobileProfileHero(
        bannerUrl = profileSummary?.bannerUrl.orEmpty(),
        bannerWideUrl = profileSummary?.bannerWideUrl,
        avatarUrl = profileSummary?.avatarUrl ?: state.avatarUrl,
        nickname = nickname,
        modifier = modifier,
        topExtra = topExtra,
        topActions = {
            AccountMobileHeroActionButton(
                icon = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.account_settings),
                onClick = onSettingsClick,
            )
            AccountMobileHeroActionButton(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.account_logout),
                onClick = onLogoutClick,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Справа остаётся место под кнопку, чтобы чипы и «О себе» не заезжали под неё.
            AccountMobileProfileIdentity(
                nickname = nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                summary = profileSummary,
                modifier = Modifier.padding(end = 56.dp),
            )
            AccountMobileProfileEditButton(
                onClick = onEditClick,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
