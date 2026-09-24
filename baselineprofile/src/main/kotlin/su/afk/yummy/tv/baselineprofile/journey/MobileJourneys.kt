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

/** Обход всех вкладок нижней навигации с прокруткой их контента и возврат на исходную. */
fun MacrobenchmarkScope.visitMainTabs() {
    val tabs = device.findObjects(By.res(Tags.MAIN_TAB))
    if (tabs.isEmpty()) return
    val initial = tabs.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    tabs.indices.filter { it != initial }.forEach { index ->
        device.findObjects(By.res(Tags.MAIN_TAB)).getOrNull(index)?.click() ?: return@forEach
        device.wait(Until.hasObject(By.scrollable(true)), SCREEN_TIMEOUT_MS / 3)
        flingList(By.scrollable(true), Direction.DOWN, Direction.UP)
    }
    device.findObjects(By.res(Tags.MAIN_TAB)).getOrNull(initial)?.click()
    waitFor(By.res(Tags.HOME_FEED))
}
