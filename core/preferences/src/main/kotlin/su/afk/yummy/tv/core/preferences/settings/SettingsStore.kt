package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.MainSettingsSnapshot
import su.afk.yummy.tv.core.model.settings.SettingsSnapshot

/**
 * Фасад над доменными хранилищами настроек ([YaniAccountSettingsStore], [PlayerSettingsStore],
 * [AppearanceSettingsStore], [CacheSettingsStore], [VideoExportSettingsStore],
 * [AppLifecycleSettingsStore], [SearchSettingsStore]) для кода, которому нужны поля сразу нескольких доменов
 * (например, [settingsSnapshot]/[mainSettingsSnapshot] агрегируют их). Однодоменным
 * потребителям следует зависеть от узкого интерфейса напрямую.
 */
interface SettingsStore :
    YaniAccountSettingsStore,
    PlayerSettingsStore,
    AppearanceSettingsStore,
    CacheSettingsStore,
    VideoExportSettingsStore,
    AppLifecycleSettingsStore,
    SearchSettingsStore {

    val settingsSnapshot: Flow<SettingsSnapshot>
    val mainSettingsSnapshot: Flow<MainSettingsSnapshot>

    companion object {
        val defaultDetailsButtonOrder: List<DetailsButtonAction>
            get() = AppearanceSettingsStore.defaultDetailsButtonOrder
        val defaultLibraryTabOrder: List<LibraryTabKind>
            get() = AppearanceSettingsStore.defaultLibraryTabOrder
    }
}
