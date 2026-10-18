package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileQuickAction

private const val ColumnsPerRow = 4

@Composable
internal fun AccountMobileQuickActionsGrid(
    actions: List<AccountMobileQuickAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.chunked(ColumnsPerRow).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { action ->
                    AccountMobileQuickActionButton(
                        title = action.title,
                        icon = action.icon,
                        onClick = action.onClick,
                        focusRequester = action.focusRequester,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(ColumnsPerRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
