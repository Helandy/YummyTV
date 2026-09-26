package su.afk.yummy.tv.feature.settings.mapper

import su.afk.yummy.tv.core.utils.formatting.formatReleaseNotes
import su.afk.yummy.tv.domain.update.model.AppReleaseNotes
import su.afk.yummy.tv.domain.update.util.compareVersions
import su.afk.yummy.tv.feature.settings.model.ReleaseNoteItem

internal fun AppReleaseNotes.toReleaseNoteItem(currentVersion: String): ReleaseNoteItem =
    ReleaseNoteItem(
        version = version.substringBefore('-'),
        date = publishedDate?.toDisplayDate(),
        notes = changelog.formatReleaseNotes(),
        isPrerelease = isPrerelease,
        isCurrent = compareVersions(version, currentVersion) == 0,
    )

/** `2026-09-20` → `20.09.2026`; нераспознанную дату показываем как есть. */
private fun String.toDisplayDate(): String {
    val parts = split('-')
    return if (parts.size == 3) parts.reversed().joinToString(".") else this
}
