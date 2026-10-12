package su.afk.yummy.tv.feature.account.common.model

import androidx.compose.ui.graphics.Color

data class ProfileWatchSlice(
    val title: String,
    val shortName: String,
    val seconds: Long,
    val color: Color,
)
