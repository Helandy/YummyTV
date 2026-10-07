package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.logging.AppLogRecordingSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отдаёт флаг записи логов и потоком реальных значений ([enabled]), и синхронно ([isEnabled]):
 * писатели логов (приёмник диагностики, обработчик падений) читают его в момент записи и ждать
 * DataStore не могут.
 *
 * До первого чтения DataStore [isEnabled] — `false`: это значение по умолчанию, поэтому на холодном
 * старте в файл ничего лишнего не попадёт.
 */
@Singleton
internal class DataStoreAppLogRecordingSettings @Inject constructor(
    settingsStore: AppLifecycleSettingsStore,
    @IoApplicationScope scope: CoroutineScope,
) : AppLogRecordingSettings {

    override val enabled: Flow<Boolean> = settingsStore.appLogRecordingEnabled.distinctUntilChanged()

    private val current: StateFlow<Boolean> =
        enabled.stateIn(scope, SharingStarted.Eagerly, false)

    override val isEnabled: Boolean get() = current.value
}
