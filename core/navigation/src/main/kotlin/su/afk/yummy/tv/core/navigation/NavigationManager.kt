package su.afk.yummy.tv.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.tab.SideTab

class NavigationManager(
    val roots: Map<SideTab, NavKey>,
    initialTab: SideTab,
) {
    var currentTab: SideTab by mutableStateOf(initialTab)
        private set

    private var stacks: Map<SideTab, MutableList<NavKey>> by mutableStateOf(
        SideTab.entries.associateWith { mutableStateListOf<NavKey>() }
    )

    val backStack: MutableList<NavKey>
        get() = stacks.getValue(currentTab)

    fun stack(tab: SideTab): MutableList<NavKey> = stacks.getValue(tab)

    init {
        SideTab.entries.forEach { tab ->
            val stack = stacks.getValue(tab)
            if (stack.isEmpty()) stack += roots.getValue(tab)
        }
    }

    fun attachBackStacks(tabStacks: Map<SideTab, NavBackStack<NavKey>>) {
        SideTab.entries.forEach { tab ->
            val root = roots.getValue(tab)
            val currentStack = stacks.getValue(tab)
            val saveableStack = tabStacks.getValue(tab)
            if (currentStack.hasPendingNavigation(root) && saveableStack.isInitialStack(root)) {
                saveableStack.replaceWith(currentStack)
            }
        }
        stacks = tabStacks
    }

    fun switchTab(tab: SideTab, reselectPopToRoot: Boolean = true) {
        if (tab == currentTab) {
            if (reselectPopToRoot) popToRoot()
            return
        }
        currentTab = tab
    }

    fun restoreTab(tab: SideTab) {
        currentTab = tab
    }

    fun navigate(dest: NavKey) {
        backStack += dest
    }

    fun replace(dest: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = dest
        else backStack += dest
    }

    fun back() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return
        }
        if (currentTab != SideTab.HOME) currentTab = SideTab.HOME
    }

    fun backTwo() {
        repeat(2) {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            else {
                if (backStack.isEmpty()) backStack += roots.getValue(currentTab)
                return
            }
        }
        if (backStack.isEmpty()) backStack += roots.getValue(currentTab)
    }

    fun popBackTo(dest: NavKey, inclusive: Boolean = false) {
        val index = backStack.indexOf(dest)
        if (index == -1) return
        val removeFrom = if (inclusive) index else index + 1
        for (i in backStack.lastIndex downTo removeFrom) {
            backStack.removeAt(i)
        }
        if (backStack.isEmpty()) backStack += roots.getValue(currentTab)
    }

    fun popToRoot() {
        val root = roots.getValue(currentTab)
        backStack.clear()
        backStack += root
    }

    fun resetAllTabs() {
        SideTab.entries.forEach { tab ->
            stacks.getValue(tab).apply {
                clear()
                add(roots.getValue(tab))
            }
        }
        currentTab = SideTab.HOME
    }

    private fun List<NavKey>.isInitialStack(root: NavKey): Boolean =
        size == 1 && firstOrNull() == root

    private fun List<NavKey>.hasPendingNavigation(root: NavKey): Boolean =
        !isInitialStack(root)

    private fun MutableList<NavKey>.replaceWith(items: List<NavKey>) {
        clear()
        addAll(items)
    }
}
