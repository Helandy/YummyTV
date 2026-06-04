package su.afk.yummy.tv.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun interface NavRegistrar {
    fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager)
}
