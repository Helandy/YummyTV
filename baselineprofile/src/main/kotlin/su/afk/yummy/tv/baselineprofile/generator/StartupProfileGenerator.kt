package su.afk.yummy.tv.baselineprofile.generator

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.afk.yummy.tv.baselineprofile.journey.startAppAndWaitForHome
import su.afk.yummy.tv.baselineprofile.journey.targetPackageName

/**
 * Только холодный старт до загруженной главной — на телефоне и на ТВ.
 * Единственный генератор, попадающий в startup-профиль: по нему R8 раскладывает
 * код старта в первый DEX, поэтому лишних сценариев здесь быть не должно.
 */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = targetPackageName, includeInStartupProfile = true) {
        startAppAndWaitForHome()
    }
}
