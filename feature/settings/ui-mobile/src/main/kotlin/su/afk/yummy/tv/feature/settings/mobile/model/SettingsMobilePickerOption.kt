package su.afk.yummy.tv.feature.settings.mobile.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.label

internal data class SettingsMobilePickerOption<T>(
    val value: T,
    val label: String,
    val hint: String = "",
    /** Переопределяет цвет текста подписи — например, чтобы показать реальный цвет субтитров. */
    val labelColor: Color? = null,
    /** Значок варианта в цветной плашке; без него строка остаётся только с подписью. */
    val icon: ImageVector? = null,
    val iconColor: Color? = null,
)
