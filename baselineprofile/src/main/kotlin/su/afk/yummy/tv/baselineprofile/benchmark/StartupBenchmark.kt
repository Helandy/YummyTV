package su.afk.yummy.tv.baselineprofile.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import su.afk.yummy.tv.baselineprofile.journey.chooseInterfaceIfAsked
import su.afk.yummy.tv.baselineprofile.journey.startAppAndWaitForHome
import su.afk.yummy.tv.baselineprofile.journey.targetPackageName

/** Холодный старт до загруженной главной: без профиля против с профилем. */
@RunWith(Parameterized::class)
class StartupBenchmark(
    @Suppress("unused") private val modeName: String,
    private val compilationMode: CompilationMode,
) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startup() = rule.measureRepeated(
        packageName = targetPackageName,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = {
            // выбор интерфейса делается до замера, иначе первая итерация померит диалог
            pressHome()
            startActivityAndWait()
            chooseInterfaceIfAsked()
        },
    ) {
        startAppAndWaitForHome()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes() = profileCompilationModes
    }
}
