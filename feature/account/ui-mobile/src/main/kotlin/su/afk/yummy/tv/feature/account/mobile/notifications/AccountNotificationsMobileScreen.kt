package su.afk.yummy.tv.feature.account.mobile.notifications

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationOpeningLoader
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.notifications.NotificationsState
import su.afk.yummy.tv.feature.account.presentation.R

/** Отдельный экран уведомлений: «Мои подписки», бейджи типов, действия над списком и сам список. */
@Composable
fun AccountNotificationsMobileScreen(
    state: NotificationsState.State,
    effect: Flow<NotificationsState.Effect>,
    onEvent: (NotificationsState.Event) -> Unit,
) {
    var isOpeningNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { onEvent(NotificationsState.Event.ScreenShown) }
    BackHandler { onEvent(NotificationsState.Event.BackSelected) }

    // Ошибку открытия уведомления показываем тостом, а не блоком с «Повторить».
    val context = LocalContext.current
    val openFailedMessage = AccountUiError.OPEN_NOTIFICATION_FAILED.accountErrorMessage()
    LaunchedEffect(state.error) {
        if (state.error == AccountUiError.OPEN_NOTIFICATION_FAILED && openFailedMessage != null) {
            Toast.makeText(context, openFailedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.account_tab_notifications),
                onBack = { onEvent(NotificationsState.Event.BackSelected) },
            )
        },
    ) {
        Column(modifier = Modifier.mobileContentMaxWidth().fillMaxWidth()) {
            val error = state.error?.takeIf { it != AccountUiError.OPEN_NOTIFICATION_FAILED }
            error?.accountErrorMessage()?.let { message ->
                MobileMessage(
                    title = message,
                    description = state.errorDetails,
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(CoreR.string.common_retry),
                    onAction = { onEvent(NotificationsState.Event.RefreshSelected) },
                    fillMaxSize = false,
                )
            }
            AccountMobileNotificationsTab(
                state = state,
                onEvent = onEvent,
                onOpenNotification = { isOpeningNotification = true },
            )
        }
    }
    AccountMobileNotificationOpeningLoader(
        state = state,
        isOpeningLatched = isOpeningNotification,
        onLatchReset = { isOpeningNotification = false },
    )
}
