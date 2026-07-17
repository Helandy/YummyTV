package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileFaqButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountMobileNavigationButton(
        title = stringResource(R.string.account_faq),
        icon = Icons.Default.Info,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
internal fun AccountMobileSitePagesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountMobileNavigationButton(
        title = stringResource(R.string.account_site_pages),
        icon = Icons.Default.Language,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
internal fun AccountMobileSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    AccountMobileNavigationButton(
        title = stringResource(R.string.account_settings),
        icon = Icons.Default.Settings,
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
    )
}

@Composable
internal fun AccountMobileNavigationButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val focusModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(focusModifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
