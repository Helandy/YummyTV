package su.afk.yummy.tv.feature.details.details.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction

internal enum class ButtonStyle { Filled, Outlined, Normal }

internal data class ButtonData(
    val action: DetailsButtonAction,
    val label: String,
    val icon: ImageVector,
    val style: ButtonStyle,
    val onClick: () -> Unit,
    val overrideColor: Color? = null,
    val enabled: Boolean = true,
    /** Короткий счётчик в углу иконки (комментарии, рецензии). */
    val badge: String? = null,
)
