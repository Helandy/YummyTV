package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.mobile.utils.label

@Composable
internal fun AccountMobileHeader(
    state: AccountState.State,
    profileSummary: UserProfileSummary?,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountMobileSurfacePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val nickname =
                    profileSummary?.nickname?.ifBlank { state.nickname } ?: state.nickname
                val avatarUrl = profileSummary?.avatarUrl ?: state.avatarUrl
                AccountMobileAvatar(avatarUrl = avatarUrl, nickname = nickname)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    profileSummary?.let { summary ->
                        AccountMobileProfileMetaLine(
                            label = stringResource(R.string.account_profile_registered),
                            value = summary.registerDateSeconds.formatProfileDate(),
                        )
                        AccountMobileProfileMetaLine(
                            label = stringResource(R.string.account_profile_birth_date),
                            value = summary.birthDateSeconds.formatProfileDate(),
                        )
                        AccountMobileProfileMetaLine(
                            label = stringResource(R.string.account_profile_sex),
                            value = summary.sex.label(),
                        )
                    }
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.account_logout),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountMobileProfileMetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        text = stringResource(R.string.account_profile_meta_line, label, value),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
