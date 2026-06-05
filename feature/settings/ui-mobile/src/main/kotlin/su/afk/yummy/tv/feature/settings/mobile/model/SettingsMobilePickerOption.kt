package su.afk.yummy.tv.feature.settings.mobile.model

internal data class SettingsMobilePickerOption<T>(
    val value: T,
    val label: String,
    val hint: String,
)
