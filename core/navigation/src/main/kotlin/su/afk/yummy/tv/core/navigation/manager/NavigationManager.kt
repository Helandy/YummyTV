package su.afk.yummy.tv.core.navigation.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.root.RootTab

/**
 * Держит два уровня back stack'ов:
 * - per-root [stacks] — обычная навигация внутри одного из [RootTab] (нижних/боковых табов);
 * - [appBackStack] — общий оверлейный стек поверх табов (например, полноэкранные экраны
 *   вне таб-бара). Пока он не пуст, [backStack] и производные от него операции
 *   (`back`, `backTwo`, `popBackTo`, ...) работают именно с ним, а не с текущим табом.
 *
 * Стек — синглтон, общий для Mobile и TV UI: в него может попасть ключ, зарегистрированный
 * только в наборе регистраторов другой платформы (см. fallback в `AppNavHost`), это штатная
 * ситуация, а не ошибка.
 */
internal class NavigationManager(
    override val roots: Map<RootTab, NavKey>,
    initialRoot: RootTab,
) : INavigationManager {
    override var appBackStack: MutableList<NavKey> by mutableStateOf(mutableStateListOf())
        private set

    override var currentRoot: RootTab by mutableStateOf(initialRoot)
        private set

    private var stacks: Map<RootTab, MutableList<NavKey>> by mutableStateOf(
        RootTab.entries.associateWith { mutableStateListOf<NavKey>() }
    )

    override val backStack: MutableList<NavKey>
        get() = if (appBackStack.isNotEmpty()) appBackStack else stacks.getValue(currentRoot)

    override fun stack(root: RootTab): MutableList<NavKey> = stacks.getValue(root)

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

    override fun switchRoot(root: RootTab, reselectPopToRoot: Boolean) {
        if (root == currentRoot) {
            if (reselectPopToRoot) popToRoot()
            return
        }
        currentRoot = root
    }

    override fun restoreRoot(root: RootTab) {
        currentRoot = root
    }

    override fun replaceRoot(root: RootTab, dest: NavKey) {
        appBackStack.clear()
        stacks.getValue(root).apply {
            clear()
            add(dest)
        }
        currentRoot = root
    }

    override fun navigate(dest: NavKey) {
        if (backStack.lastOrNull() == dest) return
        backStack += dest
    }

    /** Почему деталь заменяется, а не кладётся сверху — см. [INavigationManager.navigateDetail]. */
    override fun navigateDetail(dest: NavKey) {
        val top = backStack.lastOrNull()
        // Другая деталь того же типа сверху бывает только в двухпанельной раскладке, где список
        // виден рядом: меняем её, чтобы «назад» закрывал деталь, а не листал историю деталей.
        if (top != null && top != dest && top::class == dest::class) replace(dest) else navigate(dest)
    }

    override fun navigateApp(dest: NavKey) {
        if (appBackStack.lastOrNull() == dest) return
        appBackStack += dest
    }

    override fun replace(dest: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = dest
        else backStack += dest
    }

    override fun back() {
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

    override fun backTwo() {
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

    override fun popBackTo(dest: NavKey, inclusive: Boolean) {
        val index = backStack.indexOf(dest)
        if (index == -1) return
        val removeFrom = if (inclusive) index else index + 1
        for (i in backStack.lastIndex downTo removeFrom) {
            backStack.removeAt(i)
        }
        if (appBackStack.isNotEmpty()) return
        if (backStack.isEmpty()) backStack += roots.getValue(currentRoot)
    }

    override fun popToRoot() {
        appBackStack.clear()
        val root = roots.getValue(currentRoot)
        backStack.clear()
        backStack += root
    }

    override fun resetAllRoots() {
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
