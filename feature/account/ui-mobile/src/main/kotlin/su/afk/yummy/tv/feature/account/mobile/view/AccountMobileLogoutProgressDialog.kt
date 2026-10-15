package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Модальный лоадер выхода: блокирует интерфейс и «назад», пока сессия не очищена. */
@Composable
internal fun AccountMobileLogoutProgressDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        CircularProgressIndicator()
    }
}
