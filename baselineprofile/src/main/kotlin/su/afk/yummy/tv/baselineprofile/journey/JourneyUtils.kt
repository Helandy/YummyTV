package su.afk.yummy.tv.baselineprofile.journey

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

/** Таймаут ожидания экранов с сетевыми данными. */
internal const val SCREEN_TIMEOUT_MS = 15_000L

/** Таймаут ожидания мелких UI-реакций (диалог, переход фокуса). */
internal const val UI_TIMEOUT_MS = 5_000L

/**
 * Test tags приложения (Modifier.testTag, видны как resource-id благодаря
 * testTagsAsResourceId в MobileActivity/TvActivity). Держать в синхроне с UI.
 */
internal object Tags {
    const val HOME_FEED = "home_feed"
    const val HOME_SEARCH = "home_search"
    const val ANIME_CARD = "anime_card"
    const val DETAILS_ROOT = "details_root"
    const val MAIN_TAB = "main_tab"
}

/** Пакет тестируемого приложения: у release и releaseDebug он разный. */
val targetPackageName: String
    get() = requireNotNull(InstrumentationRegistry.getArguments().getString("targetAppId")) {
        "targetAppId не передан раннеру (см. baselineprofile/build.gradle.kts)"
    }

fun isTelevisionDevice(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().context
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

internal fun MacrobenchmarkScope.waitFor(selector: BySelector, timeoutMs: Long = SCREEN_TIMEOUT_MS): Boolean =
    device.wait(Until.hasObject(selector), timeoutMs)

/**
 * Серия fling-жестов по списку. Списки пересоздаются, когда догружаются данные, —
 * поэтому список ищется заново перед каждым жестом.
 */
internal fun MacrobenchmarkScope.flingList(selector: BySelector, vararg directions: Direction) {
    directions.forEach { direction ->
        val list = device.findObject(selector) ?: return
        try {
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(direction)
        } catch (_: StaleObjectException) {
            // список перерисовался посреди жеста — следующий шаг найдёт новый
        }
        device.waitForIdle()
    }
}

internal fun MacrobenchmarkScope.flingDownAndBack(selector: BySelector = By.scrollable(true)) =
    flingList(selector, Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.UP, Direction.UP, Direction.UP)
