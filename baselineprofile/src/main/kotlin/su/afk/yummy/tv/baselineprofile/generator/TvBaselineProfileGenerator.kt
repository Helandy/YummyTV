package su.afk.yummy.tv.baselineprofile.generator

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.afk.yummy.tv.baselineprofile.journey.browseHomeGrid
import su.afk.yummy.tv.baselineprofile.journey.isTelevisionDevice
import su.afk.yummy.tv.baselineprofile.journey.openFocusedDetailsAndBack
import su.afk.yummy.tv.baselineprofile.journey.startAppAndWaitForHome
import su.afk.yummy.tv.baselineprofile.journey.targetPackageName
import su.afk.yummy.tv.baselineprofile.journey.visitMenuSections

/**
 * Ключевые пути ТВ-интерфейса пультом: сетка главной, детали тайтла, разделы бокового меню.
 * Только публичные экраны — вход в аккаунт не нужен; плеер не трогаем (зависит от балансеров).
 */
@RunWith(AndroidJUnit4::class)
class TvBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // на «чужом» устройстве просто ничего не собираем: Assume раннер засчитывает как падение
        if (!isTelevisionDevice()) return
        rule.collect(packageName = targetPackageName, includeInStartupProfile = false) {
            startAppAndWaitForHome()
            browseHomeGrid()
            openFocusedDetailsAndBack()
            visitMenuSections()
        }
    }
}
