package su.afk.yummy.tv.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.root.RootTab

class NavigationManager(
    val roots: Map<RootTab, NavKey>,
    initialRoot: RootTab,
) {
    var appBackStack: MutableList<NavKey> by mutableStateOf(mutableStateListOf())
        private set

    var currentRoot: RootTab by mutableStateOf(initialRoot)
        private set

    private var stacks: Map<RootTab, MutableList<NavKey>> by mutableStateOf(
        RootTab.entries.associateWith { mutableStateListOf<NavKey>() }
    )

    val backStack: MutableList<NavKey>
        get() = if (appBackStack.isNotEmpty()) appBackStack else stacks.getValue(currentRoot)

    fun stack(root: RootTab): MutableList<NavKey> = stacks.getValue(root)

    init {
        RootTab.entries.forEach { root ->
            val stack = stacks.getValue(root)
            if (stack.isEmpty()) stack += roots.getValue(root)
        }
    }

    fun attachBackStacks(
        appBackStack: NavBackStack<NavKey>,
        rootStacks: Map<RootTab, NavBackStack<NavKey>>,
    ) {
        if (this.appBackStack.hasPendingAppNavigation() && appBackStack.isInitialAppStack()) {
            appBackStack.replaceWith(this.appBackStack)
        }

        RootTab.entries.forEach { rootTab ->
            val root = roots.getValue(rootTab)
            val currentStack = stacks.getValue(rootTab)
            val saveableStack = rootStacks.getValue(rootTab)
            if (currentStack.hasPendingNavigation(root) && saveableStack.isInitialStack(root)) {
                saveableStack.replaceWith(currentStack)
            }
        }

        this.appBackStack = appBackStack
        stacks = rootStacks
    }

    fun switchRoot(root: RootTab, reselectPopToRoot: Boolean = true) {
        if (root == currentRoot) {
            if (reselectPopToRoot) popToRoot()
            return
        }
        currentRoot = root
    }

    fun restoreRoot(root: RootTab) {
        currentRoot = root
    }

    fun replaceRoot(root: RootTab, dest: NavKey) {
        appBackStack.clear()
        stacks.getValue(root).apply {
            clear()
            add(dest)
        }
        currentRoot = root
    }

    fun navigate(dest: NavKey) {
        if (backStack.lastOrNull() == dest) return
        backStack += dest
    }

    fun navigateApp(dest: NavKey) {
        if (appBackStack.lastOrNull() == dest) return
        appBackStack += dest
    }

    fun replace(dest: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = dest
        else backStack += dest
    }

    fun back() {
        if (appBackStack.isNotEmpty()) {
            appBackStack.removeAt(appBackStack.lastIndex)
            return
        }
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return
        }
        if (currentRoot != RootTab.HOME) currentRoot = RootTab.HOME
    }

    fun backTwo() {
        if (appBackStack.isNotEmpty()) {
            repeat(2) {
                if (appBackStack.isNotEmpty()) appBackStack.removeAt(appBackStack.lastIndex)
            }
            return
        }
        repeat(2) {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            else {
                if (backStack.isEmpty()) backStack += roots.getValue(currentRoot)
                return
            }
        }
        if (backStack.isEmpty()) backStack += roots.getValue(currentRoot)
    }

    fun popBackTo(dest: NavKey, inclusive: Boolean = false) {
        val index = backStack.indexOf(dest)
        if (index == -1) return
        val removeFrom = if (inclusive) index else index + 1
        for (i in backStack.lastIndex downTo removeFrom) {
            backStack.removeAt(i)
        }
        if (appBackStack.isNotEmpty()) return
        if (backStack.isEmpty()) backStack += roots.getValue(currentRoot)
    }

    fun popToRoot() {
        appBackStack.clear()
        val root = roots.getValue(currentRoot)
        backStack.clear()
        backStack += root
    }

    fun resetAllRoots() {
        appBackStack.clear()
        RootTab.entries.forEach { rootTab ->
            stacks.getValue(rootTab).apply {
                clear()
                add(roots.getValue(rootTab))
            }
        }
        currentRoot = RootTab.HOME
    }

    private fun List<NavKey>.isInitialStack(root: NavKey): Boolean =
        size == 1 && firstOrNull() == root

    private fun List<NavKey>.isInitialAppStack(): Boolean = isEmpty()

    private fun List<NavKey>.hasPendingNavigation(root: NavKey): Boolean =
        !isInitialStack(root)

    private fun List<NavKey>.hasPendingAppNavigation(): Boolean = isNotEmpty()

    private fun MutableList<NavKey>.replaceWith(items: List<NavKey>) {
        clear()
        addAll(items)
    }
}
