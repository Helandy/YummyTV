package su.afk.yummy.tv.feature.glossary.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.glossary.GlossaryViewModel
import su.afk.yummy.tv.feature.glossary.IMobileGlossaryEntry
import su.afk.yummy.tv.feature.glossary.mobile.GlossaryMobileScreen
import su.afk.yummy.tv.feature.glossary.navigator.GlossaryDestination
import javax.inject.Inject

class GlossaryNavRegistrar @Inject constructor() : IMobileGlossaryEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<GlossaryDestination> {
                val vm = hiltViewModel<GlossaryViewModel>()
                ScreenNavigator(vm) { state, effect, onEvent ->
                    GlossaryMobileScreen(state, effect, onEvent)
                }
            }
        }
}
