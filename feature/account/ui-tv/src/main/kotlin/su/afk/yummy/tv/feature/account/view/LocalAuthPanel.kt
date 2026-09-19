package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun LocalAuthPanel(
    state: LocalAuthServerState,
    onBack: () -> Unit,
    onRefreshPin: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // Без явного запроса фокус уходит из панели и открывается боковое меню.
    val backFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val refreshFocusRequester = remember { FocusRequester() }
    val isTerminalError = state is LocalAuthServerState.Error

    LaunchedEffect(backFocusRequester, isTerminalError) {
        // На терминальной ошибке ведущее действие — выпустить новый код.
        requestFocusUntilTimeout(if (isTerminalError) refreshFocusRequester else backFocusRequester)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.account_local_auth_pairing_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        when (state) {
            is LocalAuthServerState.Pairing -> {
                Text(
                    text = stringResource(R.string.account_local_auth_pairing_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = state.pin.chunked(LocalAuthCode.GROUP_SIZE).joinToString(" "),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        letterSpacing = 8.sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                val lastError = state.lastError
                if (lastError != null) {
                    Text(
                        text = lastError.message(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.account_local_auth_attempts_left,
                            state.attemptsLeft,
                            state.attemptsLeft,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LocalAuthServerState.Transferring -> {
                Text(
                    text = stringResource(R.string.account_local_auth_transferring),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LocalAuthServerState.Success -> {
                Text(
                    text = stringResource(R.string.account_local_auth_success),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is LocalAuthServerState.Error -> {
                Text(
                    text = state.reason.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.account_local_auth_refresh_pin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            LocalAuthServerState.Idle -> {
                // Should not happen when panel is shown
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isTerminalError) {
                AccountAction(
                    label = stringResource(R.string.account_local_auth_refresh_pin),
                    onClick = onRefreshPin,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(refreshFocusRequester),
                )
            }
            AccountAction(
                label = stringResource(R.string.account_back),
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(backFocusRequester),
            )
        }
    }
}

@Composable
private fun LocalAuthError.message(): String = stringResource(
    when (this) {
        LocalAuthError.INVALID_PIN -> R.string.account_local_auth_error_invalid_pin
        LocalAuthError.PIN_EXPIRED -> R.string.account_local_auth_error_pin_expired
        LocalAuthError.TOO_MANY_ATTEMPTS -> R.string.account_local_auth_error_too_many_attempts
        LocalAuthError.SERVICE_UNAVAILABLE -> R.string.account_local_auth_error_service_unavailable
        LocalAuthError.PERMISSION_DENIED -> R.string.account_local_auth_error_permission_denied
        LocalAuthError.SIGN_IN_FAILED -> R.string.account_local_auth_error_sign_in_failed
    },
)
