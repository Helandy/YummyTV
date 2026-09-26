package su.afk.yummy.tv.feature.settings.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/** Загрузка истории релизов для «Что нового»: грузится при первом открытии и живёт до конца экрана. */
@Immutable
sealed interface ReleaseNotesStatus {
    data object Idle : ReleaseNotesStatus
    data object Loading : ReleaseNotesStatus
    data object Error : ReleaseNotesStatus
    data class Loaded(val items: ImmutableList<ReleaseNoteItem>) : ReleaseNotesStatus
}
