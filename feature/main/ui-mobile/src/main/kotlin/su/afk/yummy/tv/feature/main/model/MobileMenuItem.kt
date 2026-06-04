package su.afk.yummy.tv.feature.main.model

import androidx.compose.ui.graphics.vector.ImageVector

internal data class MobileMenuItem<T>(
    val label: String,
    val destination: T,
    val icon: ImageVector,
)
