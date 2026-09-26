package su.afk.yummy.tv.feature.settings.model

import androidx.compose.runtime.Immutable

/**
 * Релиз в истории изменений. [version] без суффикса `-beta` (бета отмечена [isPrerelease]),
 * [date] уже отформатирована (`20.09.2026`), [notes] — changelog без markdown-разметки.
 * [isCurrent] — установленная сейчас версия.
 */
@Immutable
data class ReleaseNoteItem(
    val version: String,
    val date: String?,
    val notes: String,
    val isPrerelease: Boolean,
    val isCurrent: Boolean,
)
