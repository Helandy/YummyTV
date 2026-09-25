package su.afk.yummy.tv.core.navigation.registrar

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.manager.INavigationManager

/**
 * Точка регистрации экранов фичи в общем `entryProvider`. Фичи не реализуют его напрямую, а
 * наследуют платформенный контракт из своего `api` (`ITvXxxEntry` / `IMobileXxxEntry`), который
 * явно собирается в `TvNavigationHolder` / `MobileNavigationHolder`.
 * `nav` типизирован интерфейсом [INavigationManager], а не конкретным `NavigationManager`
 * (тот `internal` в этом модуле) — фичам не нужен доступ к internal-wiring вроде
 * `attachBackStacks`, только навигационные операции.
 */
fun interface NavRegistrar {
    fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager)
}
