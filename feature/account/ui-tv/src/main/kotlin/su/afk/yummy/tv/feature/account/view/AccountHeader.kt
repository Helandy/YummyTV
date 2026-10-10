@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.utils.label

@Composable
internal fun AccountHeader(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    // Вниз с «Выйти» — на выбранный таб, иначе фокус улетает в первую карточку статистики.
    downFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AccountAvatar(avatarUrl = state.avatarUrl, nickname = state.nickname)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = state.nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.profileSummary?.let { summary -> AccountHeaderProfileMeta(summary = summary) }
                val unreadCount = state.notificationCounts.sumOf { it.count }
                if (unreadCount > 0) {
                    AccountHeaderMetaLine(
                        icon = Icons.Filled.Notifications,
                        value = stringResource(R.string.account_unread_count, unreadCount),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountAction(
                label = stringResource(R.string.account_logout),
                icon = Icons.AutoMirrored.Filled.Logout,
                hint = stringResource(R.string.account_logout_hint),
                onClick = { onEvent(AccountState.Event.LogoutSelected) },
                modifier = Modifier.focusProperties {
                    downFocusRequester?.let { down = it }
                },
            )
        }
    }
}

@Composable
private fun AccountHeaderProfileMeta(summary: UserProfileSummary) {
    AccountHeaderMetaLine(
        icon = Icons.Filled.CalendarMonth,
        value = summary.registerDateSeconds.formatProfileDate(),
    )
    AccountHeaderMetaLine(
        icon = Icons.Filled.Person,
        value = summary.sex.label(),
    )
}

@Composable
private fun AccountHeaderMetaLine(
    icon: ImageVector,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (value.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
