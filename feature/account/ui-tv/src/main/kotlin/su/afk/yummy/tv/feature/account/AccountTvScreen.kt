package su.afk.yummy.tv.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick

@Composable
fun AccountTvScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    BackHandler { onEvent(AccountState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = TvScreenPadding.Vertical),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.74f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.account_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            val isLoggedIn = state.accessToken.isNotBlank()
            Text(
                text = if (isLoggedIn) {
                    stringResource(R.string.account_signed_in)
                } else {
                    stringResource(R.string.account_signed_out)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (isLoggedIn) {
                AccountInfoRow(
                    label = state.nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                    hint = "ID ${state.userId}",
                )
                AccountAction(
                    label = stringResource(R.string.account_refresh),
                    hint = if (state.isLoading) stringResource(R.string.account_loading) else stringResource(R.string.account_refresh_hint),
                    onClick = { onEvent(AccountState.Event.RefreshProfileSelected) },
                )
                AccountAction(
                    label = stringResource(R.string.account_logout),
                    hint = stringResource(R.string.account_logout_hint),
                    onClick = { onEvent(AccountState.Event.LogoutSelected) },
                )
            } else {
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
                    hint = if (state.isLoading) stringResource(R.string.account_loading) else stringResource(R.string.account_login_hint),
                    onClick = { onEvent(AccountState.Event.LoginSelected) },
                )
                Text(
                    text = stringResource(R.string.account_captcha_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, hint: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(
                color = if (focused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = shape,
            )
            .tvFocusableClick(onClick = onClick, interactionSource = interactionSource, shape = shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
