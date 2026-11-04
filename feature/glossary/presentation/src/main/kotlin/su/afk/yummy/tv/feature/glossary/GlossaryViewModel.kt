package su.afk.yummy.tv.feature.glossary

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import javax.inject.Inject

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
) : BaseViewModel<GlossaryState.State, GlossaryState.Event, GlossaryState.Effect>() {
    override fun createInitialState() = GlossaryState.State()

    override fun onEvent(event: GlossaryState.Event) {
        when (event) {
            GlossaryState.Event.BackSelected -> nav.back()
            is GlossaryState.Event.QueryChanged -> setState { copy(query = event.query) }
            is GlossaryState.Event.CategoryToggled -> setState {
                copy(
                    collapsedCategories = if (event.category in collapsedCategories) {
                        collapsedCategories - event.category
                    } else {
                        collapsedCategories + event.category
                    },
                )
            }
        }
    }
}
