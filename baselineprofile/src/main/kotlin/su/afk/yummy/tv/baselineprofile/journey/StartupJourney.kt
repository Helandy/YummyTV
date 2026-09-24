package su.afk.yummy.tv.baselineprofile.journey

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * Холодный старт через лаунчер-роутер до загруженной главной.
 * При первом запуске роутер спрашивает интерфейс — выбирается подходящий устройству.
 */
fun MacrobenchmarkScope.startAppAndWaitForHome() {
    pressHome()
    startActivityAndWait()
    chooseInterfaceIfAsked()
    dismissPermissionDialog()
    waitFor(By.res(Tags.HOME_FEED))
    device.waitForIdle()
}

/**
 * Выбор интерфейса сохраняется в настройках, поэтому достаточно сделать его один раз
 * до замеров: иначе первая итерация бенчмарка померит диалог выбора, а не старт.
 */
fun MacrobenchmarkScope.chooseInterfaceIfAsked() {
    device.wait(Until.hasObject(By.pkg(packageName).depth(0)), UI_TIMEOUT_MS)
    val labels = if (isTelevisionDevice()) {
        listOf("TV interface", "ТВ-интерфейс")
    } else {
        listOf("Mobile interface", "Мобильный интерфейс")
    }
    val pattern = Pattern.compile(labels.joinToString("|") { Pattern.quote(it) })
    val option = device.findObject(By.text(pattern)) ?: return
    option.click()
    device.wait(Until.gone(By.text(pattern)), UI_TIMEOUT_MS)
}

/** Системный запрос уведомлений (API 33+) перекрывает UI — отказываем, если он есть. */
private fun MacrobenchmarkScope.dismissPermissionDialog() {
    val deny = device.wait(
        Until.findObject(By.res(Pattern.compile(".*:id/permission_deny.*"))),
        UI_TIMEOUT_MS / 2,
    )
    deny?.click()
}
