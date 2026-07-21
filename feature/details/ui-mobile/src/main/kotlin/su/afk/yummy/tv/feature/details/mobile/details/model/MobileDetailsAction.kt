package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction

internal data class MobileDetailsAction(
    val action: DetailsButtonAction,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
