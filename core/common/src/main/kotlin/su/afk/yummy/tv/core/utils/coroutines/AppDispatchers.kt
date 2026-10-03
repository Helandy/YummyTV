package su.afk.yummy.tv.core.utils.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/** Диспатчеры приложения: внедряются вместо прямого обращения к `Dispatchers`, чтобы код был тестируемым. */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
