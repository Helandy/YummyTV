package su.afk.yummy.tv.feature.account.mobile.account.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.account.common.utils.label

internal data class AccountMobileProfileCounterItem(
    val label: String,
    val count: Int,
    val color: Color,
    val icon: ImageVector,
)
