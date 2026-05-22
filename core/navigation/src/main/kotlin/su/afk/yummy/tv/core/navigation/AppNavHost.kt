package su.afk.yummy.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import su.afk.yummy.tv.core.navigation.tab.SideTab

@Composable
fun AppNavHost(
    navManager: NavigationManager,
    registrars: Set<@JvmSuppressWildcards NavRegistrar>,
    modifier: Modifier = Modifier,
) {
    val homeBackStack = key(SideTab.HOME) {
        rememberNavBackStack(navManager.roots.getValue(SideTab.HOME))
    }
    val searchBackStack = key(SideTab.SEARCH) {
        rememberNavBackStack(navManager.roots.getValue(SideTab.SEARCH))
    }
    val top100BackStack = key(SideTab.TOP100) {
        rememberNavBackStack(navManager.roots.getValue(SideTab.TOP100))
    }
    val libraryBackStack = key(SideTab.LIBRARY) {
        rememberNavBackStack(navManager.roots.getValue(SideTab.LIBRARY))
    }

    val tabStacks = remember(homeBackStack, searchBackStack, top100BackStack, libraryBackStack) {
        mapOf(
            SideTab.HOME to homeBackStack,
            SideTab.SEARCH to searchBackStack,
            SideTab.TOP100 to top100BackStack,
            SideTab.LIBRARY to libraryBackStack,
        )
    }

    LaunchedEffect(homeBackStack, searchBackStack, top100BackStack, libraryBackStack) {
        navManager.attachBackStacks(tabStacks)
    }

    val provider = remember(registrars, navManager) {
        entryProvider<NavKey> {
            registrars.forEach { it.register(this, navManager) }
        }
    }

    val homeEntries = key(SideTab.HOME) {
        rememberDecoratedNavEntries(
            backStack = homeBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = provider,
        )
    }
    val searchEntries = key(SideTab.SEARCH) {
        rememberDecoratedNavEntries(
            backStack = searchBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = provider,
        )
    }
    val top100Entries = key(SideTab.TOP100) {
        rememberDecoratedNavEntries(
            backStack = top100BackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = provider,
        )
    }
    val libraryEntries = key(SideTab.LIBRARY) {
        rememberDecoratedNavEntries(
            backStack = libraryBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = provider,
        )
    }

    val entriesToShow: List<NavEntry<NavKey>> = when (navManager.currentTab) {
        SideTab.HOME -> homeEntries
        SideTab.SEARCH -> homeEntries + searchEntries
        SideTab.TOP100 -> homeEntries + top100Entries
        SideTab.LIBRARY -> homeEntries + libraryEntries
    }

    NavDisplay(
        entries = entriesToShow,
        onBack = { navManager.back() },
        modifier = modifier,
    )
}
