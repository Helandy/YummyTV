package su.afk.yummy.tv.feature.account.mobile.model

import androidx.compose.ui.graphics.Color

internal data class ProfileWatchSlice(
    val title: String,
    val shortName: String,
    val seconds: Long,
    val color: Color,
)
