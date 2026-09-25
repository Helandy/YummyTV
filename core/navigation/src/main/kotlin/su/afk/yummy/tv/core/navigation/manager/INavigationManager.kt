package su.afk.yummy.tv.core.navigation.manager

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.root.RootTab

/**
 * Узкая, немутабельная граница [NavigationManager] для потребителей вне модуля навигации
 * (core:error, core:deeplink и т.п.), которым не нужен доступ к internal wiring
 * (см. [NavigationManager.attachBackStacks]).
 */
interface INavigationManager {
    val backStack: List<NavKey>
    val appBackStack: List<NavKey>
    val currentRoot: RootTab
    val roots: Map<RootTab, NavKey>

    fun navigate(dest: NavKey)
    fun navigateApp(dest: NavKey)

    /**
     * Открыть деталь из списка (пост, чат, рецензию) с учётом двухпанельной раскладки.
     *
     * На широком окне список и деталь видны одновременно. С обычным [navigate] выбор поста A,
     * а затем B дал бы стек `[Posts, PostA, PostB]`: справа виден B, но «назад» вернул бы к A,
     * хотя список всё время на экране. Здесь деталь того же типа сверху **заменяется**:
     * стек становится `[Posts, PostB]`, и «назад» сразу закрывает деталь.
     *
     * - сверху деталь того же класса, но другая (другой id) — [replace];
     * - сверху тот же самый ключ — [navigate], который дубль и так отбрасывает;
     * - сверху список или экран другого типа — обычный [navigate].
     *
     * На телефоне и TV деталь закрывает список целиком, и кликнуть в нём, пока деталь открыта,
     * нельзя: сверху в момент выбора всегда сам список, поэтому это просто [navigate].
     *
     * Сравнивается только класс ключа. Вызывать из экранов списков; переходы внутри детали
     * к детали того же типа (например, «похожие посты» из поста) должны идти через [navigate],
     * иначе они заменят текущую деталь, а не встанут поверх.
     */
    fun navigateDetail(dest: NavKey)
    fun replace(dest: NavKey)
    fun back()
    fun backTwo()
    fun popBackTo(dest: NavKey, inclusive: Boolean = false)
    fun popToRoot()
    fun switchRoot(root: RootTab, reselectPopToRoot: Boolean = true)
    fun restoreRoot(root: RootTab)
    fun replaceRoot(root: RootTab, dest: NavKey)
    fun resetAllRoots()
    fun stack(root: RootTab): List<NavKey>
}
