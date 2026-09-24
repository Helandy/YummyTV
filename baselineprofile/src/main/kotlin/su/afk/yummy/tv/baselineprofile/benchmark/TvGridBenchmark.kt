package su.afk.yummy.tv.baselineprofile.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import su.afk.yummy.tv.baselineprofile.journey.browseHomeGrid
import su.afk.yummy.tv.baselineprofile.journey.isTelevisionDevice
import su.afk.yummy.tv.baselineprofile.journey.startAppAndWaitForHome
import su.afk.yummy.tv.baselineprofile.journey.targetPackageName

/** Плавность первой DPAD-навигации по сетке главной на ТВ: без профиля против с профилем. */
@RunWith(Parameterized::class)
class TvGridBenchmark(
    @Suppress("unused") private val modeName: String,
    private val compilationMode: CompilationMode,
) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun browseHome() {
        if (!isTelevisionDevice()) return
        rule.measureRepeated(
            packageName = targetPackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            // без startupMode: с ним Macrobenchmark убивает процесс после setupBlock.
            // Холодный старт делаем сами — меряется первая прокрутка после запуска.
            setupBlock = {
                killProcess()
                startAppAndWaitForHome()
            },
        ) {
            browseHomeGrid(rows = 6)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes() = profileCompilationModes
    }
}
