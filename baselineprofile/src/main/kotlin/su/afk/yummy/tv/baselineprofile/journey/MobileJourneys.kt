package su.afk.yummy.tv.baselineprofile.journey

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

private const val SEARCH_QUERY = "naruto"

/** Прокрутка ленты главной вниз и обратно к началу. */
fun MacrobenchmarkScope.scrollHomeFeed() {
    waitFor(By.res(Tags.HOME_FEED))
    flingDownAndBack(By.res(Tags.HOME_FEED))
}

/** Главная → первая карточка тайтла → прокрутка деталей → назад. */
fun MacrobenchmarkScope.openFirstDetailsAndBack() {
    val card = device.wait(Until.findObject(By.res(Tags.ANIME_CARD)), SCREEN_TIMEOUT_MS) ?: return
    card.click()
    if (!waitFor(By.res(Tags.DETAILS_ROOT))) return
    flingList(By.res(Tags.DETAILS_ROOT), Direction.DOWN, Direction.DOWN, Direction.UP)
    device.pressBack()
    waitFor(By.res(Tags.HOME_FEED))
}

/** Поиск с главной: ввод запроса, ожидание выдачи, возврат. */
fun MacrobenchmarkScope.searchFromHome() {
    val entry = device.wait(Until.findObject(By.res(Tags.HOME_SEARCH)), UI_TIMEOUT_MS) ?: return
    entry.click()
    val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), UI_TIMEOUT_MS)
    if (field != null) {
        field.text = SEARCH_QUERY
        device.wait(Until.hasObject(By.scrollable(true)), SCREEN_TIMEOUT_MS)
        device.waitForIdle()
        device.pressBack() // клавиатура
    }
    device.pressBack()
    waitFor(By.res(Tags.HOME_FEED))
}

/**
 * Обход всех вкладок нижней навигации и возврат на исходную. На каждой вкладке ждём
 * выбора вкладки и загрузки данных (список с контентом), прокручиваем его вниз и обратно.
 */
fun MacrobenchmarkScope.visitMainTabs() {
    val tabs = device.findObjects(By.res(Tags.MAIN_TAB))
    if (tabs.isEmpty()) return
    val initial = tabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    tabs.indices.filter { it != initial }.forEach { index ->
        device.findObjects(By.res(Tags.MAIN_TAB)).getOrNull(index)?.click() ?: return@forEach
        device.wait(Until.hasObject(By.res(Tags.MAIN_TAB).selected(true)), UI_TIMEOUT_MS)
        device.wait(Until.hasObject(By.scrollable(true)), SCREEN_TIMEOUT_MS)
        device.waitForIdle()
        Thread.sleep(TAB_SETTLE_MS) // догрузка данных и картинок
        flingDownAndBack()
    }
    device.findObjects(By.res(Tags.MAIN_TAB)).getOrNull(initial)?.click()
    waitFor(By.res(Tags.HOME_FEED))
}

/**
 * Главная → первая карточка → «Смотреть» → плеер: ждём поток, показываем и прячем
 * оверлей, перематываем жестом, даём немного поиграть, затем выходим до главной.
 * Поток приходит от внешнего балансера: если не получен, сценарий тихо завершается
 * (профиль просто не включит код воспроизведения).
 */
fun MacrobenchmarkScope.watchEpisodeInMobilePlayer() {
    val card = device.wait(Until.findObject(By.res(Tags.ANIME_CARD)), SCREEN_TIMEOUT_MS) ?: return
    card.click()
    if (!waitFor(By.res(Tags.DETAILS_ROOT))) return
    val watch = device.wait(Until.findObject(By.res(Tags.WATCH_BUTTON)), SCREEN_TIMEOUT_MS)
    if (watch == null) {
        returnToHome()
        return
    }
    watch.click()
    if (pickUntilPlayerOpens() && waitFor(By.res(Tags.PLAYER_VIEW), PLAYER_STREAM_TIMEOUT_MS)) {
        device.waitForIdle()
        Thread.sleep(PLAYBACK_MS)
        exercisePlayerGestures()
        Thread.sleep(PLAYBACK_MS)
    }
    returnToHome()
}

private fun MacrobenchmarkScope.exercisePlayerGestures() {
    val width = device.displayWidth
    val height = device.displayHeight
    // тап — оверлей управления показывается, второй — прячется
    repeat(2) {
        device.click(width / 2, height / 2)
        device.waitForIdle()
        Thread.sleep(GESTURE_PAUSE_MS)
    }
    // горизонтальный свайп — жест перемотки
    device.swipe(width * 2 / 5, height / 2, width * 3 / 5, height / 2, SWIPE_STEPS)
    device.waitForIdle()
    // двойной тап у края — быстрая перемотка
    repeat(2) { device.click(width * 4 / 5, height / 2) }
    device.waitForIdle()
    // тап — оверлей на экране, чтобы отрисовались его элементы
    device.click(width / 2, height / 2)
    device.waitForIdle()
    Thread.sleep(GESTURE_PAUSE_MS)
}

/** Назад, пока не окажемся на главной (плеер → детали → главная); лишний BACK не нажимаем. */
private fun MacrobenchmarkScope.returnToHome() {
    repeat(MAX_BACK_PRESSES) {
        if (device.hasObject(By.res(Tags.HOME_FEED))) return
        device.pressBack()
        device.waitForIdle()
    }
}

private const val TAB_SETTLE_MS = 1_500L
private const val PLAYBACK_MS = 3_000L
private const val GESTURE_PAUSE_MS = 600L
private const val SWIPE_STEPS = 20
private const val MAX_BACK_PRESSES = 4
