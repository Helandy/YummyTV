package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import su.afk.yummy.tv.feature.account.presentation.R

/** Выбор раздела FAQ: от разработчика, о сайте или глоссарий. */
@Composable
internal fun AccountMobileFaqChooserDialog(
    onDeveloperFaq: () -> Unit,
    onSiteFaq: () -> Unit,
    onGlossary: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_faq_group),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
                FaqChooserRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.account_faq),
                    hint = stringResource(R.string.account_faq_developer_hint),
                    onClick = onDeveloperFaq,
                )
                FaqChooserRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.account_site_pages),
                    hint = stringResource(R.string.account_faq_site_hint),
                    onClick = onSiteFaq,
                )
                FaqChooserRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.account_glossary),
                    hint = stringResource(R.string.account_faq_glossary_hint),
                    onClick = onGlossary,
                )
            }
        }
    }
}

@Composable
private fun FaqChooserRow(
    icon: ImageVector,
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
