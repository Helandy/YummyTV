package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.AppBrandIcon
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage

@Composable
internal fun AccountMobileLoginCard(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountMobileSurfacePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppBrandIcon(modifier = Modifier.size(76.dp))
                    Text(
                        text = stringResource(R.string.account_mobile_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    val errorMessage = state.errorMessage ?: state.error.accountErrorMessage()
                    errorMessage?.let { AccountMobileInfoText(it, isError = true) }
                }
            }
            Text(
                text = stringResource(R.string.account_signed_out),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
            if (!state.isCaptchaRequired) {
                Button(
                    onClick = { onEvent(AccountState.Event.LoginSelected) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.account_login))
                    }
                }
                TextButton(
                    onClick = { onEvent(AccountState.Event.PasswordResetSelected) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.account_forgot_password))
                }
                Button(
                    onClick = { onEvent(AccountState.Event.RegistrationSelected) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.account_mobile_register))
                }
            }
            Text(
                text = stringResource(R.string.account_mobile_register_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isCaptchaRequired) {
                key(state.captchaChallengeId) {
                    AccountMobileHCaptcha(
                        siteKey = state.captchaSiteKey,
                        onSolved = { onEvent(AccountState.Event.CaptchaSolved(it)) },
                        onExpired = { onEvent(AccountState.Event.CaptchaExpired) },
                        onFailed = { onEvent(AccountState.Event.CaptchaFailed()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountMobileInfoText(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
