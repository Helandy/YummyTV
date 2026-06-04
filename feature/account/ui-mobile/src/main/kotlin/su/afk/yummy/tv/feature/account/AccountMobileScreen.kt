package su.afk.yummy.tv.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileScreen
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    MobileScreen(title = stringResource(R.string.account_mobile_title), onBack = { onEvent(AccountState.Event.BackSelected) }) { padding ->
        if (!state.isSignedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.login,
                    onValueChange = { onEvent(AccountState.Event.LoginChanged(it)) },
                    label = { Text(stringResource(R.string.account_mobile_login)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(AccountState.Event.PasswordChanged(it)) },
                    label = { Text(stringResource(R.string.account_mobile_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onEvent(AccountState.Event.LoginSelected) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.isLoading) {
                            stringResource(R.string.account_mobile_signing_in)
                        } else {
                            stringResource(R.string.account_mobile_sign_in)
                        },
                    )
                }
                state.error?.let { Text(it) }
                if (state.isCaptchaRequired) {
                    Text(stringResource(R.string.account_mobile_captcha_required))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(state.nickname.ifBlank { stringResource(R.string.account_mobile_profile) })
                    Button(onClick = { onEvent(AccountState.Event.RefreshHubSelected) }) {
                        Text(stringResource(R.string.account_mobile_refresh))
                    }
                    Button(onClick = { onEvent(AccountState.Event.LogoutSelected) }) {
                        Text(stringResource(R.string.account_mobile_logout))
                    }
                }
                item { Text(stringResource(R.string.account_mobile_notifications)) }
                items(state.notifications, key = { it.id }) { notification ->
                    Button(
                        onClick = { onEvent(AccountState.Event.NotificationSelected(notification.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(notification.title)
                    }
                }
            }
        }
    }
}
