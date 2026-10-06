package su.afk.yummy.tv.feature.playersetup.mobile.model

internal data class PlayerSetupMobilePickerOption<T>(
    val value: T,
    val label: String,
    val hint: String = "",
)
