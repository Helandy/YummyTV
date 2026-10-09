package su.afk.yummy.tv.baselineprofile.journey

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import kotlin.math.abs

/**
 * Смещения по пунктам бокового меню между посещениями, от «Главной»: расписание, поиск,
 * затем вниз до коллекций, новостей, топа и библиотеки, и обратно на главную.
 * Порядок пунктов — в `tvMenuItems` (feature:main:ui-tv).
 */
private val MENU_VISIT_STEPS = listOf(-1, -1, 3, 1, 1, 1, -4)

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
 * Разделы бокового меню пультом: BACK из контента возвращает фокус на выбранный пункт меню
 * (TvMainScaffold), DPAD_UP/DOWN переходит к нужному пункту, CENTER открывает раздел.
 * В каждом разделе ждём данные и прокручиваем контент пультом.
 */
fun MacrobenchmarkScope.visitMenuSections() {
    MENU_VISIT_STEPS.forEach { step ->
        device.pressBack()
        device.waitForIdle()
        repeat(abs(step)) {
            if (step < 0) device.pressDPadUp() else device.pressDPadDown()
        }
        device.pressDPadCenter()
        device.waitForIdle()
        Thread.sleep(SECTION_SETTLE_MS) // догрузка данных и картинок
        device.pressDPadRight()
        repeat(3) { device.pressDPadDown() }
        device.waitForIdle()
    }
}

/**
 * Главная → карточка в фокусе → детали → «Смотреть» (кнопка в фокусе при входе) → плеер:
 * ждём поток, показываем панель управления, двигаем фокус по ней и ставим на паузу.
 * Если поток не получен (внешний балансер), сценарий тихо завершается.
 */
fun MacrobenchmarkScope.watchEpisodeInTvPlayer() {
    device.pressDPadDown()
    device.pressDPadCenter()
    if (!waitFor(By.res(Tags.DETAILS_ROOT))) return
    waitFor(By.res(Tags.WATCH_BUTTON))
    device.wait(Until.hasObject(By.res(Tags.WATCH_BUTTON).focused(true)), UI_TIMEOUT_MS)
    device.pressDPadCenter()
    if (pickUntilPlayerOpens() && waitFor(By.res(Tags.PLAYER_VIEW), PLAYER_STREAM_TIMEOUT_MS)) {
        device.waitForIdle()
        Thread.sleep(PLAYBACK_MS)
        exercisePlayerControls()
        Thread.sleep(PLAYBACK_MS)
    }
    returnToHome()
}

private fun MacrobenchmarkScope.exercisePlayerControls() {
    device.pressDPadDown() // панель управления
    device.waitForIdle()
    repeat(2) { device.pressDPadRight() }
    device.pressDPadCenter() // пауза
    device.waitForIdle()
    device.pressDPadCenter() // продолжить
    device.pressDPadLeft()
    device.waitForIdle()
    device.pressDPadUp()
    device.waitForIdle()
}

/** Назад, пока не окажемся на главной (плеер → детали → главная); лишний BACK не нажимаем. */
private fun MacrobenchmarkScope.returnToHome() {
    repeat(MAX_BACK_PRESSES) {
        if (device.hasObject(By.res(Tags.HOME_FEED))) return
        device.pressBack()
        device.waitForIdle()
    }
}

private const val SECTION_SETTLE_MS = 1_500L
private const val PLAYBACK_MS = 3_000L
private const val MAX_BACK_PRESSES = 4
