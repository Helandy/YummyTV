package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.InlineError
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHCaptcha
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.registration.RegistrationState
import su.afk.yummy.tv.feature.account.utils.YANI_HCAPTCHA_SITE_KEY
import su.afk.yummy.tv.feature.account.common.utils.label

@Composable
internal fun RegistrationForm(
    state: RegistrationState.State,
    onEvent: (RegistrationState.Event) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(RegistrationState.Event.EmailChanged(it)) },
            label = { Text(stringResource(R.string.password_reset_email)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress },
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = { onEvent(RegistrationState.Event.UsernameChanged(it)) },
            label = { Text(stringResource(R.string.account_username_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.NewUsername },
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(RegistrationState.Event.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.account_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.NewPassword },
        )

        if (!state.isCaptchaRequired) {
            Button(
                onClick = { onEvent(RegistrationState.Event.RegisterSelected) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    YummyLoadingIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.account_mobile_register))
                }
            }
        }

        if (state.isCaptchaRequired) {
            key(state.captchaChallengeId) {
                AccountMobileHCaptcha(
                    siteKey = YANI_HCAPTCHA_SITE_KEY,
                    onSolved = { onEvent(RegistrationState.Event.CaptchaSolved(it)) },
                    onExpired = { onEvent(RegistrationState.Event.CaptchaExpired) },
                    onFailed = { onEvent(RegistrationState.Event.CaptchaFailed()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }

        RegistrationTermsNotice()

        Text(
            text = stringResource(R.string.account_registration_site_fallback_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        val errorMessage = state.errorMessage ?: state.error.accountErrorMessage()
        if (errorMessage != null) {
            InlineError(
                message = errorMessage,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
