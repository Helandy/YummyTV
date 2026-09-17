package su.afk.yummy.tv.feature.account.mobile.registration

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.feature.account.account.YANI_HCAPTCHA_SITE_KEY
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHCaptcha
import su.afk.yummy.tv.feature.account.registration.RegistrationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationMobileScreen(
    state: RegistrationState.State,
    effect: Flow<RegistrationState.Effect>,
    onEvent: (RegistrationState.Event) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val captchaHint = stringResource(R.string.account_captcha_required_toast)

    LaunchedEffect(effect) {
        effect.collect { effectItem ->
            when (effectItem) {
                RegistrationState.Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }

                RegistrationState.Effect.ShowCaptchaHint -> Toast.makeText(context, captchaHint, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_registration_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(RegistrationState.Event.BackSelected) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) {
        if (state.isSuccess) {
            RegistrationSuccessView(onBack = { onEvent(RegistrationState.Event.BackSelected) })
        } else {
            RegistrationForm(state, onEvent)
        }
    }
}

@Composable
private fun RegistrationSuccessView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.account_registration_success_hint),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.account_registration_back_to_login))
        }
    }
}

@Composable
private fun RegistrationForm(
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
            label = { Text(stringResource(R.string.account_email_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = { onEvent(RegistrationState.Event.UsernameChanged(it)) },
            label = { Text(stringResource(R.string.account_username_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(RegistrationState.Event.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.account_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!state.isCaptchaRequired) {
            Button(
                onClick = { onEvent(RegistrationState.Event.RegisterSelected) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
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

        val errorMessage = state.errorMessage ?: state.error.accountErrorMessage()
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
