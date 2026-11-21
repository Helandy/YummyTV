package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileBlockingLoading
import su.afk.yummy.tv.feature.account.notifications.NotificationsState

/**
 * Блокирующий лоадер на время открытия уведомления. [isOpeningLatched] — защёлка со стороны экрана:
 * VM гасит isNotificationOpening за кадр до navigate, и без неё лоадер мигал бы. Защёлка
 * сбрасывается, когда экран ушёл в фон, при ошибке открытия и по таймауту.
 */
@Composable
internal fun AccountMobileNotificationOpeningLoader(
    state: NotificationsState.State,
    isOpeningLatched: Boolean,
    onLatchReset: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onLatchReset()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.isNotificationOpening, state.error) {
        if (!state.isNotificationOpening && state.error != null) onLatchReset()
    }
    LaunchedEffect(isOpeningLatched) {
        if (isOpeningLatched) {
            delay(OPENING_LOADER_TIMEOUT_MS)
            onLatchReset()
        }
    }
    if (state.isNotificationOpening || isOpeningLatched) {
        MobileBlockingLoading()
    }
}

private const val OPENING_LOADER_TIMEOUT_MS = 15_000L
