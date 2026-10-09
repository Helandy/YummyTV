package su.afk.yummy.tv.baselineprofile.generator

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.afk.yummy.tv.baselineprofile.journey.isTelevisionDevice
import su.afk.yummy.tv.baselineprofile.journey.startAppAndWaitForHome
import su.afk.yummy.tv.baselineprofile.journey.targetPackageName
import su.afk.yummy.tv.baselineprofile.journey.watchEpisodeInTvPlayer

/**
 * Путь к воспроизведению на ТВ: главная → детали → плеер.
 * Вынесен в отдельный генератор: поток берётся у внешнего балансера, и сбой сети не должен
 * ронять остальные сценарии. При недоступном потоке профиль просто не включит код плеера.
 */
@RunWith(AndroidJUnit4::class)
class TvPlayerProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // на «чужом» устройстве просто ничего не собираем: Assume раннер засчитывает как падение
        if (!isTelevisionDevice()) return
        rule.collect(packageName = targetPackageName, includeInStartupProfile = false) {
            startAppAndWaitForHome()
            watchEpisodeInTvPlayer()
        }
    }
}
