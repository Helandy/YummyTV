package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.mobile.R

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
