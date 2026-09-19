package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.compose.ui.graphics.Color

internal data class MobilePickerItem(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val views: Int? = null,
    val color: Color? = null,
    val enabled: Boolean = true,
    /** Название пункта акцентным цветом — как в шторках скачивания. */
    val accentTitle: Boolean = false,
    val onClick: () -> Unit,
)
