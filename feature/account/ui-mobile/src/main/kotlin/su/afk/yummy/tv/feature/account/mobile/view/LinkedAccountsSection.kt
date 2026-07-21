package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.label

@Composable
internal fun LinkedAccountsSection(
    linkedAccounts: Set<LinkedAccountProvider>,
    unlinkingAccount: LinkedAccountProvider?,
    onUnlink: (LinkedAccountProvider) -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.profile_linked_accounts),
                style = MaterialTheme.typography.titleMedium,
            )
            if (linkedAccounts.isEmpty()) {
                Text(
                    stringResource(R.string.profile_linked_accounts_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinkedAccountProvider.entries
                    .filter(linkedAccounts::contains)
                    .forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(provider.label(), modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { onUnlink(provider) },
                                enabled = unlinkingAccount == null,
                            ) {
                                if (unlinkingAccount == provider) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(stringResource(R.string.profile_unlink_account))
                                }
                            }
                        }
                    }
            }
        }
    }
}
