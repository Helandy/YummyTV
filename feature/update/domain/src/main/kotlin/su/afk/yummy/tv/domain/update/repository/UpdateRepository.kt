package su.afk.yummy.tv.domain.update.repository

import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.model.AppReleaseNotes

interface UpdateRepository {

    /**
     * Самый новый опубликованный релиз или null, если релизов нет либо у него нет APK.
     * Pre-release учитываются только при [includePrerelease].
     */
    suspend fun latestRelease(currentVersion: String, includePrerelease: Boolean): AppRelease?

    /**
     * Релизы не новее [currentVersion], отсортированные от новых к старым. Бросает исключение,
     * если релизы получить не удалось, — чтобы экран отличил ошибку от пустой истории. Pre-release учитываются только при [includePrerelease].
     */
    suspend fun releaseHistory(currentVersion: String, includePrerelease: Boolean): List<AppReleaseNotes>
}
