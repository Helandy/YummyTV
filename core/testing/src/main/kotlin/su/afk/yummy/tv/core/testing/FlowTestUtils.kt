package su.afk.yummy.tv.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Подписывается на [flow] до конца теста и складывает эмиссии в возвращаемый список —
 * так проверяют одноразовые effect, которые не хранятся в `SharedFlow`.
 */
fun <T> TestScope.collectEmissions(flow: Flow<T>): List<T> {
    val items = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(items) }
    return items
}
