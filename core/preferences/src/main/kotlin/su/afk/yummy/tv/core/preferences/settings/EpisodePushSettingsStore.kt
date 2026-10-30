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

    /** Включены ли пуши о новых сериях в приложении (поверх системного разрешения). */
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)

    /** Включён ли локальный пуш о новых сериях тайтлов из списков пользователя. */
    val libraryEnabled: Flow<Boolean>

    suspend fun setLibraryEnabled(enabled: Boolean)

    /**
     * Ключи серий (`animeId:episode`) и отметка базовой линии, которые локальный воркер уже
     * учёл. Каждый прогон целиком заменяет набор: так он не растёт и сам забывает серии,
     * вышедшие из окна расписания.
     */
    val knownLibraryEpisodeKeys: Flow<Set<String>>

    suspend fun setKnownLibraryEpisodeKeys(keys: Set<String>)

    /**
     * Когда по тайтлу последний раз показывали пуш о новой серии (animeId → epoch-секунды),
     * серверный или локальный: общий дедуп двух воркеров. Старые записи чистятся при записи.
     */
    val recentPushes: Flow<Map<Int, Long>>

    suspend fun recordPush(animeId: Int, seconds: Long)

    suspend fun addKnownNotificationIds(ids: Set<Int>)
}
