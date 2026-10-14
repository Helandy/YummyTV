package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileQuickAction

private const val ColumnsPerRow = 3

@Composable
internal fun AccountMobileQuickActionsGrid(
    actions: List<AccountMobileQuickAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.chunked(ColumnsPerRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { action ->
                    AccountMobileNavigationButton(
                        title = action.title,
                        icon = action.icon,
                        onClick = action.onClick,
                        focusRequester = action.focusRequester,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
