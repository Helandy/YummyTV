package su.afk.yummy.tv.baselineprofile.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
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

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun startup() = rule.measureRepeated(
        packageName = targetPackageName,
        // StartupTimingMetric даёт и timeToFullDisplayMs — главная зовёт reportFullyDrawn;
        // App.* — шаги Application.onCreate, см. YummyTvApplication
        metrics = listOf(
            StartupTimingMetric(),
            TraceSectionMetric("App.%", TraceSectionMetric.Mode.Sum),
        ),
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
