package su.afk.yummy.tv.core.designsystem.mobile.bar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MobileBottomBarDefaults {

    /** Высота самого бара без системного инсета: иконки без подписей, 80.dp из M3 избыточны. */
    val BarHeight: Dp = 56.dp

    val ExtraContentBottomPadding: Dp = 16.dp

    /** Дополнительный отступ для плавающих элементов над оверлейной панелью. */
    val floatingBarExtraPadding: Dp
        @Composable get() = if (LocalMobileNavigationLayout.current == MobileNavigationLayout.FloatingBottomBar) {
            BarHeight
        } else {
            0.dp
        }

    /**
     * Отступ снизу для скроллящегося контента корневых экранов табов. Нижний бар сам занимает
     * место вместе с инсетом системной навигации, поэтому над ним нужен только зазор; при рейке
     * или скрытой навигации контент доходит до края окна и сам уходит от системного инсета.
     */
    val contentBottomPadding: Dp
        @Composable get() = when (LocalMobileNavigationLayout.current) {
            MobileNavigationLayout.BottomBar -> ExtraContentBottomPadding
            MobileNavigationLayout.FloatingBottomBar -> navigationBarsInset() + BarHeight + ExtraContentBottomPadding
            MobileNavigationLayout.Rail,
            MobileNavigationLayout.Hidden,
            -> navigationBarsInset() + ExtraContentBottomPadding
        }

    /**
     * То же, но для корня таба, который остаётся в композиции под экраном поверх него (рецензии
     * с главной). Бар скрывается и возвращается вместе с экраном, и вьюпорт списка меняется на
     * высоту бара и системного инсета. Если отступ при этом меняется меньше, LazyList зажимает
     * сохранённую прокрутку, и после возврата нижние кнопки уходят под бар. Поэтому при скрытом
     * баре добавляем отступ той же высоты: предел прокрутки не меняется.
     */
    val rootContentBottomPadding: Dp
        @Composable get() = when (LocalMobileNavigationLayout.current) {
            MobileNavigationLayout.BottomBar -> ExtraContentBottomPadding
            MobileNavigationLayout.Rail -> navigationBarsInset() + ExtraContentBottomPadding
            MobileNavigationLayout.FloatingBottomBar,
            MobileNavigationLayout.Hidden -> navigationBarsInset() + BarHeight + ExtraContentBottomPadding
        }

    @Composable
    private fun navigationBarsInset(): Dp =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}
