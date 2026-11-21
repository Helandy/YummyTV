package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileQuickAction

/**
 * Ряд быстрых действий одним контейнером с тонкой обводкой: равные по ширине сегменты, между ними
 * тонкие разделители. Лёгкий вид без заливок и пустых слотов.
 */
@Composable
internal fun AccountMobileQuickActionsRow(
    actions: List<AccountMobileQuickAction>,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .border(BorderStroke(1.dp, outline), shape),
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(outline),
                )
            }
            AccountMobileQuickActionSegment(
                action = action,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}
