package su.afk.yummy.tv.baselineprofile.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode

/**
 * Режимы, между которыми сравнивается выигрыш профиля:
 * - `None` — как свежая установка без профиля: только интерпретатор и JIT;
 * - `BaselineProfile` — установка с нашим профилем (так APK ставит profileinstaller).
 */
internal val profileCompilationModes: List<Array<Any>> = listOf(
    arrayOf("None", CompilationMode.None()),
    arrayOf("BaselineProfile", CompilationMode.Partial(BaselineProfileMode.Require)),
)
