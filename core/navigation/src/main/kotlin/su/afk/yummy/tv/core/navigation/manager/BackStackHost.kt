package su.afk.yummy.tv.core.navigation.manager

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.root.RootTab

/**
 * Internal wiring навигации для `AppNavHost`: подкладывает менеджеру сохраняемые
 * (`rememberNavBackStack`) стеки, созданные в композиции.
 *
 * Только для `AppNavHost`; фичам нужен [INavigationManager].
 */
interface BackStackHost {
    fun attachBackStacks(
        appBackStack: NavBackStack<NavKey>,
        rootStacks: Map<RootTab, NavBackStack<NavKey>>,
    )
}
