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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileScreen

@Composable
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    MobileScreen(title = "Аккаунт", onBack = { onEvent(AccountState.Event.BackSelected) }) { padding ->
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
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(AccountState.Event.PasswordChanged(it)) },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onEvent(AccountState.Event.LoginSelected) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isLoading) "Вход..." else "Войти")
                }
                state.error?.let { Text(it) }
                if (state.isCaptchaRequired) {
                    Text("Для входа требуется captcha. TV-версия пока открывает полный поток captcha.")
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
                    Text(state.nickname.ifBlank { "Профиль" })
                    Button(onClick = { onEvent(AccountState.Event.RefreshHubSelected) }) { Text("Обновить") }
                    Button(onClick = { onEvent(AccountState.Event.LogoutSelected) }) { Text("Выйти") }
                }
                item { Text("Уведомления") }
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
