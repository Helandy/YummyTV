package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.mobile.R

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
