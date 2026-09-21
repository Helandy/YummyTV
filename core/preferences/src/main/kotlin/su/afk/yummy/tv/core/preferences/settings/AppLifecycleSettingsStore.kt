package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot

/** Разовые/lifecycle-флаги приложения: watch-next, support-prompt, объявления, версия установки. */
interface AppLifecycleSettingsStore {

    val watchNextEnabled: Flow<Boolean>

    /** Бета-канал: проверка обновлений учитывает и pre-release сборки. */
    val betaUpdatesEnabled: Flow<Boolean>

    val supportPromptSnapshot: Flow<SupportPromptSnapshot>

    /** Идентификатор последнего объявления, которое пользователь закрыл кнопкой ОК. */
    val lastSeenAnnouncementId: Flow<String>

    /** Запрашивалось ли уже разрешение на уведомления через гейт (чтобы не спрашивать повторно). */
    val notificationPermissionRequested: Flow<Boolean>

    suspend fun setWatchNextEnabled(enabled: Boolean)
    suspend fun setBetaUpdatesEnabled(enabled: Boolean)
    suspend fun ensureSupportPromptInstallTimeInitialized()
    suspend fun dismissSupportPrompt()

    /** Помечает объявление [id] как просмотренное, чтобы больше его не показывать. */
    suspend fun markAnnouncementSeen(id: String)

    /** Помечает, что разрешение на уведомления уже запрашивалось. */
    suspend fun markNotificationPermissionRequested()

    /** Returns `true` when [versionCode] differs from the previously started one. */
    suspend fun markStartedVersion(versionCode: Int): Boolean
}
