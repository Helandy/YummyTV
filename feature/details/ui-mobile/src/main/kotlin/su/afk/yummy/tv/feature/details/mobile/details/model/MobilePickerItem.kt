package su.afk.yummy.tv.feature.details.mobile.details.model

internal data class MobilePickerItem(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val views: Int? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)
