package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow

/** Локальный пуш о новых сериях по подпискам — уже учтённые воркером уведомления. */
interface EpisodePushSettingsStore {

    /**
     * Id уведомлений `ProfileNotification` (лента `GET /profile/notifications`), которые воркер
     * уже учёл — либо показал по ним локальный пуш, либо зафиксировал как базовую линию при
     * первом запуске. Используется вместо «последнего увиденного id», т.к. порядок и
     * монотонность id в ленте нигде не гарантированы.
     */
    val knownNotificationIds: Flow<Set<Int>>

    suspend fun addKnownNotificationIds(ids: Set<Int>)
}
