package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AccountMobileStatSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    AccountMobileSurfacePanel {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
        content()
    }
}
