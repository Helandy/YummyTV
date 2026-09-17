package su.afk.yummy.tv.feature.account.mobile.localauth

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.feature.account.localauth.LocalAuthState
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAuthMobileScreen(
    state: LocalAuthState.State,
    effect: Flow<LocalAuthState.Effect>,
    onEvent: (LocalAuthState.Event) -> Unit,
) {
    val context = LocalContext.current
    val transferSuccessMessage = stringResource(R.string.account_local_auth_transfer_success)
    val permissionDeniedMessage = stringResource(R.string.account_local_auth_permission_denied)

    // На Android 13+ обнаружение по NSD требует NEARBY_WIFI_DEVICES.
    val nearbyDevicesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onEvent(LocalAuthState.Event.PermissionResult(granted))
        if (!granted) {
            Toast.makeText(context, permissionDeniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        onEvent(LocalAuthState.Event.ScreenOpened)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nearbyDevicesLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            onEvent(LocalAuthState.Event.PermissionResult(granted = true))
        }
    }

    LaunchedEffect(effect) {
        effect.collect { effectItem ->
            when (effectItem) {
                LocalAuthState.Effect.TransferSuccess ->
                    Toast.makeText(context, transferSuccessMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_local_auth_login_on_tv)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(LocalAuthState.Event.BackSelected) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.account_local_auth_discovery_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.account_local_auth_discovery_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.devices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.host) },
                        modifier = Modifier.clickable {
                            onEvent(LocalAuthState.Event.DeviceSelected(device))
                        },
                        trailingContent = {
                            if (state.selectedDevice?.id == device.id) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            if (state.selectedDevice != null) {
                OutlinedTextField(
                    value = state.pin,
                    onValueChange = { onEvent(LocalAuthState.Event.PinChanged(it)) },
                    label = { Text(stringResource(R.string.account_local_auth_enter_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !state.isTransferring,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error.accountErrorMessage()?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { onEvent(LocalAuthState.Event.TransferSelected) },
                enabled = state.canTransfer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isTransferring) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.account_local_auth_transfer_button))
                }
            }
        }
    }
}
