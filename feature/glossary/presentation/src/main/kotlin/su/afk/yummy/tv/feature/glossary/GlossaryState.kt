package su.afk.yummy.tv.feature.glossary

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory

class GlossaryState {
    /** Тексты терминов статичны и лежат в ресурсах, в состоянии только запрос и свёрнутые разделы. */
    data class State(
        val query: String = "",
        val collapsedCategories: PersistentSet<GlossaryCategory> = persistentSetOf(),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event

        data class QueryChanged(val query: String) : Event

        data class CategoryToggled(val category: GlossaryCategory) : Event
    }

    sealed interface Effect : UiEffect
}
