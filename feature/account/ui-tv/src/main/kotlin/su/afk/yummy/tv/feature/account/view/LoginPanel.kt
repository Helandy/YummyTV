package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.AppBrandIcon
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage

@Composable
internal fun LoginPanel(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.74f)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppBrandIcon(modifier = Modifier.size(92.dp))
        AccountTitle()
        Text(
            text = stringResource(R.string.account_signed_out),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedTextField(
            value = state.login,
            onValueChange = { onEvent(AccountState.Event.LoginChanged(it)) },
            placeholder = { Text(stringResource(R.string.account_login_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AccountState.Event.PasswordChanged(it)) },
            placeholder = { Text(stringResource(R.string.account_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        AccountAction(
            label = stringResource(R.string.account_login),
            hint = if (state.isLoading) stringResource(R.string.account_loading) else stringResource(
                R.string.account_login_hint
            ),
            onClick = { onEvent(AccountState.Event.LoginSelected) },
            modifier = if (initialFocusRequester != null) {
                Modifier.focusRequester(initialFocusRequester)
            } else {
                Modifier
            },
        )
        if (state.isCaptchaRequired) {
            key(state.captchaChallengeId) {
                CaptchaChallenge(
                    state = state,
                    onSolved = { onEvent(AccountState.Event.CaptchaSolved(it)) },
                    onExpired = { onEvent(AccountState.Event.CaptchaExpired) },
                    onFailed = { onEvent(AccountState.Event.CaptchaFailed(it)) },
                )
            }
        }
        ErrorText(state.error.accountErrorMessage())
    }
}
