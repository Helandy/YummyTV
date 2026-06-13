package su.afk.yummy.tv.feature.details.details.model

internal data class MobilePickerItem(
    val key: String,
    val title: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)
