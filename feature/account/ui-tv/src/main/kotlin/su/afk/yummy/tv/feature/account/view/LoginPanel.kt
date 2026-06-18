package su.afk.yummy.tv.feature.account.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val loginFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val loginButtonFocusRequester = remember { FocusRequester() }
    var loginEditing by remember { mutableStateOf(false) }
    var passwordEditing by remember { mutableStateOf(false) }
    val panelOffsetY by animateDpAsState(
        targetValue = when {
            passwordEditing -> (-190).dp
            loginEditing -> (-80).dp
            else -> 0.dp
        },
        label = "LoginPanelImeOffset",
    )

    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    Column(
        modifier = modifier
            .offset(y = panelOffsetY)
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
            readOnly = !loginEditing,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                loginEditing = false
                passwordEditing = true
                passwordFocusRequester.requestFocus()
                keyboardController?.show()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(loginFocusRequester)
                .onFocusChanged {
                    if (it.isFocused && !loginEditing) {
                        keyboardController?.hide()
                    } else if (!it.isFocused) {
                        loginEditing = false
                    }
                }
                .editableTextFieldKeyEvents(
                    isEditing = loginEditing,
                    onStartEditing = {
                        loginEditing = true
                        keyboardController?.show()
                    },
                    onStopEditing = {
                        loginEditing = false
                        keyboardController?.hide()
                    },
                ),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AccountState.Event.PasswordChanged(it)) },
            placeholder = { Text(stringResource(R.string.account_password_placeholder)) },
            readOnly = !passwordEditing,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                passwordEditing = false
                keyboardController?.hide()
                loginButtonFocusRequester.requestFocus()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .onFocusChanged {
                    if (it.isFocused && !passwordEditing) {
                        keyboardController?.hide()
                    } else if (!it.isFocused) {
                        passwordEditing = false
                    }
                }
                .editableTextFieldKeyEvents(
                    isEditing = passwordEditing,
                    onStartEditing = {
                        passwordEditing = true
                        keyboardController?.show()
                    },
                    onStopEditing = {
                        passwordEditing = false
                        keyboardController?.hide()
                    },
                ),
        )
        AccountAction(
            label = stringResource(R.string.account_login),
            hint = if (state.isLoading) stringResource(R.string.account_loading) else stringResource(
                R.string.account_login_hint
            ),
            onClick = { onEvent(AccountState.Event.LoginSelected) },
            modifier = Modifier.focusRequester(loginButtonFocusRequester),
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

private fun Modifier.editableTextFieldKeyEvents(
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter -> {
            if (!isEditing) {
                onStartEditing()
                true
            } else {
                false
            }
        }

        Key.Back -> {
            if (isEditing) {
                onStopEditing()
                true
            } else {
                false
            }
        }

        else -> false
    }
}
