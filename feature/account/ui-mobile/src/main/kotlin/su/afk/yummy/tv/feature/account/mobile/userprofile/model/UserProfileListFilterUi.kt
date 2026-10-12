package su.afk.yummy.tv.feature.account.mobile.userprofile.model

import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.feature.account.common.utils.label

internal data class UserProfileListFilterUi(
    val label: String,
    val count: Int?,
    val color: Color,
)
