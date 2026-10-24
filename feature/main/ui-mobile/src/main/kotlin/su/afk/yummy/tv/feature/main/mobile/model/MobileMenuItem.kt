package su.afk.yummy.tv.feature.main.mobile.model

import androidx.compose.ui.graphics.vector.ImageVector

internal data class MobileMenuItem<T>(
    val label: String,
    val destination: T,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val hasSeasonalDecor: Boolean = false,
    /** Аватарка пользователя вместо [icon]; пусто — обычная иконка. */
    val avatarUrl: String = "",
)
