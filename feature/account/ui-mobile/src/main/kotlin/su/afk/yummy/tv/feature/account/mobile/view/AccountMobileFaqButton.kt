package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
