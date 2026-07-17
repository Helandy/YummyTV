package su.afk.yummy.tv.core.preferences.interface_mode

interface AppInterfaceModePreferences {

    val selectedMode: AppInterfaceMode?

    fun select(mode: AppInterfaceMode)
}
