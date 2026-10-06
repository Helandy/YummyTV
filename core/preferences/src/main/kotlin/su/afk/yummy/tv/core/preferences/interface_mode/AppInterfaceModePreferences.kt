package su.afk.yummy.tv.core.preferences.interface_mode

/** Синхронное хранилище: читается на старте до setContent, поэтому не DataStore. */
interface AppInterfaceModePreferences {

    val selectedMode: AppInterfaceMode?

    fun select(mode: AppInterfaceMode)

    /**
     * Нужно ли сейчас показать первичную настройку плеера. Первый вызов возвращает `true` и сразу
     * отмечает экран показанным, поэтому он открывается один раз — и новым, и текущим пользователям.
     */
    fun consumePlayerSetupPending(): Boolean
}
