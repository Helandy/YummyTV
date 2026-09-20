package su.afk.yummy.tv.feature.account.mobile.localauth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.permissions.missingLocalNetworkPermissionNames
import su.afk.yummy.tv.core.designsystem.permissions.rememberLocalNetworkPermissionGate
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
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

    // Поиск ТВ по NSD требует NEARBY_WIFI_DEVICES (Android 13+), а с Android 16 —
    // ещё и ACCESS_LOCAL_NETWORK. Перед системным запросом объясняем, зачем они.
    val permissionGate = rememberLocalNetworkPermissionGate(
        onGranted = { onEvent(LocalAuthState.Event.PermissionResult(granted = true)) },
        onDenied = {
            onEvent(
                LocalAuthState.Event.PermissionResult(
                    granted = false,
                    missing = context.missingLocalNetworkPermissionNames(),
                ),
            )
            Toast.makeText(context, permissionDeniedMessage, Toast.LENGTH_LONG).show()
        },
    )

    // Повтор поиска прямо с экрана: если разрешения уже есть, gate сразу отдаст onGranted.
    val restartSearch = {
        onEvent(LocalAuthState.Event.RetrySearchSelected)
        permissionGate.start()
    }

    LaunchedEffect(Unit) {
        onEvent(LocalAuthState.Event.ScreenOpened)
        permissionGate.start()
    }

    LocalNetworkPermissionMobileDialog(
        step = permissionGate.step,
        onConfirm = permissionGate::confirm,
        onOpenSettings = permissionGate::openSettings,
        onDismiss = permissionGate::dismiss,
    )

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
                actions = {
                    if (!state.isTransferred) {
                        IconButton(onClick = restartSearch, enabled = !state.isTransferring) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(
                                    R.string.account_local_auth_mobile_search_again_cd,
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) {
        if (state.isTransferred) {
            LocalAuthTransferredContent(onDone = { onEvent(LocalAuthState.Event.BackSelected) })
            return@BaseScreen
        }

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
                LocalAuthSearchStatus(
                    isSearching = state.isSearching,
                    isPermissionDenied = state.isPermissionDenied,
                    onRetry = restartSearch,
                    onOpenSettings = permissionGate::openSettings,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.devices, key = { it.id }) { device ->
                        LocalAuthDeviceCard(
                            device = device,
                            isSelected = state.selectedDevice?.id == device.id,
                            onClick = { onEvent(LocalAuthState.Event.DeviceSelected(device)) },
                        )
                    }
                }
            }

            if (state.selectedDevice != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.account_local_auth_enter_pin),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LocalAuthPinInput(
                        pin = state.pin,
                        enabled = !state.isTransferring,
                        isError = state.error != null,
                        onPinChange = { onEvent(LocalAuthState.Event.PinChanged(it)) },
                        onCompleted = { onEvent(LocalAuthState.Event.TransferSelected) },
                    )
                }
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

/** Пустой список: либо ещё ищем, либо нет разрешения, либо ТВ так и не отозвался. */
@Composable
private fun LocalAuthSearchStatus(
    isSearching: Boolean,
    isPermissionDenied: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isSearching) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.account_local_auth_mobile_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = if (isPermissionDenied) {
                    stringResource(R.string.account_local_auth_mobile_permission_required)
                } else {
                    stringResource(R.string.account_local_auth_discovery_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_local_auth_mobile_search_again))
        }
        // Системный диалог второй раз уже не покажут — единственный путь остаётся через настройки.
        if (isPermissionDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        R.string.account_mobile_local_network_permission_open_settings,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LocalAuthDeviceCard(
    device: DiscoveredDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
            )
            Text(
                text = device.host,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colorScheme.primary,
            )
        }
    }
}

/** Сессия уже на ТВ — вводить больше нечего, показываем итог и выход. */
@Composable
private fun LocalAuthTransferredContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = stringResource(R.string.account_local_auth_mobile_transferred_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.account_local_auth_mobile_transferred_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_local_auth_mobile_done))
        }
    }
}
