package su.afk.yummy.tv.baselineprofile.journey

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By

private const val MENU_SECTIONS_TO_VISIT = 5

/**
 * DPAD-проход по сетке главной: вниз по рядам с шагами вправо, затем обратно наверх.
 * Задевает ленивые ряды и их восстановление фокуса — как пульт реального пользователя.
 */
fun MacrobenchmarkScope.browseHomeGrid(rows: Int = 4) {
    waitFor(By.res(Tags.HOME_FEED))
    repeat(rows) {
        device.pressDPadDown()
        device.pressDPadRight()
        device.pressDPadRight()
        device.waitForIdle()
    }
    repeat(rows) { device.pressDPadUp() }
    device.waitForIdle()
}

/** Карточка в фокусе → детали → прокрутка пультом → назад на главную. */
fun MacrobenchmarkScope.openFocusedDetailsAndBack() {
    device.pressDPadDown()
    device.pressDPadCenter()
    if (!waitFor(By.res(Tags.DETAILS_ROOT))) return
    repeat(3) { device.pressDPadDown() }
    device.waitForIdle()
    device.pressBack()
    waitFor(By.res(Tags.HOME_FEED))
}

/**
 * Разделы бокового меню пультом: BACK из контента возвращает фокус в меню
 * (TvMainScaffold), DPAD_DOWN переходит к следующему пункту, CENTER открывает раздел.
 */
fun MacrobenchmarkScope.visitMenuSections() {
    repeat(MENU_SECTIONS_TO_VISIT) {
        device.pressBack()
        device.waitForIdle()
        device.pressDPadDown()
        device.pressDPadCenter()
        device.waitForIdle()
        device.pressDPadRight()
        repeat(2) { device.pressDPadDown() }
        device.waitForIdle()
    }
}
