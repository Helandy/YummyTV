package su.afk.yummy.tv.feature.main.mobile.navigation

import android.view.KeyEvent
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.search.ISearchNavigator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Глобальные сочетания клавиш мобильного UI для физической клавиатуры (планшет, Chromebook,
 * оконный режим). Вызывается из `onKeyDown` активити, то есть только для клавиш, которые Compose не
 * обработал: текстовые поля и плеер получают их первыми.
 */
@Singleton
class MobileKeyboardShortcuts @Inject internal constructor(
    private val navManager: INavigationManager,
    private val searchNavigator: ISearchNavigator,
) {

    /**
     * @param onBack системный «назад» активити: идёт через `BackHandler`'ы экранов, а не мимо
     *   них, как прямой `navManager.back()`.
     * @return `true`, если событие обработано.
     */
    fun handle(event: KeyEvent, onBack: () -> Unit): Boolean {
        if (event.repeatCount != 0) return false
        return when {
            // Не везде система превращает Esc в «назад» (в desktop-окне — нет). На корне таба
            // отдаём Esc системе, чтобы не закрывать приложение там, где этого не ждут.
            event.keyCode == KeyEvent.KEYCODE_ESCAPE && canGoBack() -> {
                onBack()
                true
            }

            event.keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed -> {
                navManager.navigate(searchNavigator.getSearchDest())
                true
            }

            else -> false
        }
    }

    private fun canGoBack(): Boolean =
        navManager.appBackStack.isNotEmpty() || navManager.backStack.size > 1
}
