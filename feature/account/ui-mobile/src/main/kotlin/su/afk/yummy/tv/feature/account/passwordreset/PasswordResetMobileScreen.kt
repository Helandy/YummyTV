package su.afk.yummy.tv.feature.account.passwordreset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.AccountMobileHCaptcha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetMobileScreen(
    state: PasswordResetState.State,
    effect: Flow<PasswordResetState.Effect>,
    onEvent: (PasswordResetState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.password_reset_title),
                onBack = { onEvent(PasswordResetState.Event.BackSelected) },
            )
        },
    ) {
        if (state.isSuccess) {
            MobileMessage(
                title = stringResource(R.string.password_reset_success),
                description = stringResource(R.string.password_reset_success_description),
                icon = Icons.Default.MarkEmailRead,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.password_reset_description))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(PasswordResetState.Event.EmailChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password_reset_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.validationError,
                    supportingText = if (state.validationError) {
                        { Text(stringResource(R.string.password_reset_invalid_email)) }
                    } else null,
                )
                Button(
                    onClick = { onEvent(PasswordResetState.Event.SubmitSelected) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.password_reset_send))
                    }
                }
                if (state.isCaptchaRequired) {
                    key(state.captchaChallengeId) {
                        AccountMobileHCaptcha(
                            siteKey = state.captchaSiteKey,
                            onSolved = { onEvent(PasswordResetState.Event.CaptchaSolved(it)) },
                            onExpired = { onEvent(PasswordResetState.Event.CaptchaExpired) },
                            onFailed = { onEvent(PasswordResetState.Event.CaptchaFailed) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                        )
                    }
                }
                if (state.requestError) {
                    Text(
                        stringResource(R.string.password_reset_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.captchaError) {
                    Text(
                        stringResource(R.string.password_reset_captcha_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
