package su.afk.yummy.tv.feature.playersetup.mobile.model

import androidx.compose.ui.graphics.vector.ImageVector

internal data class PlayerSetupMobilePickerOption<T>(
    val value: T,
    val label: String,
    val hint: String = "",
    /** Значок варианта в цветной плашке; без него строка остаётся только с подписью. */
    val icon: ImageVector? = null,
)
