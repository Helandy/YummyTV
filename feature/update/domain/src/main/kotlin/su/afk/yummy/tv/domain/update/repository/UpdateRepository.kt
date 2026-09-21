package su.afk.yummy.tv.domain.update.repository

import su.afk.yummy.tv.domain.update.model.AppRelease

interface UpdateRepository {

    /**
     * Самый новый опубликованный релиз или null, если релизов нет либо у него нет APK.
     * Pre-release учитываются только при [includePrerelease].
     */
    suspend fun latestRelease(currentVersion: String, includePrerelease: Boolean): AppRelease?
}
