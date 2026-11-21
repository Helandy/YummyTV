package su.afk.yummy.tv.feature.account.mobile.account.model

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector

internal data class AccountMobileQuickAction(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val focusRequester: FocusRequester? = null,
    /** Число на значке в углу иконки (непрочитанные); 0 — значка нет. */
    val badgeCount: Int = 0,
)
