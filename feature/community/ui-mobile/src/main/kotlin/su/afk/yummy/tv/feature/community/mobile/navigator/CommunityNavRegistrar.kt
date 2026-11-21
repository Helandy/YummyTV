package su.afk.yummy.tv.feature.community.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.community.CommunityViewModel
import su.afk.yummy.tv.feature.community.IMobileCommunityEntry
import su.afk.yummy.tv.feature.community.mobile.CommunityMobileScreen
import su.afk.yummy.tv.feature.community.navigator.CommunityDestination
import javax.inject.Inject

class CommunityNavRegistrar @Inject constructor() : IMobileCommunityEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CommunityDestination> {
                ScreenNavigator(hiltViewModel<CommunityViewModel>()) { state, effect, onEvent ->
                    CommunityMobileScreen(state, effect, onEvent)
                }
            }
        }
}
