package su.afk.yummy.tv.core.testing

import io.mockk.unmockkAll
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Rule

/**
 * Базовый класс unit-тестов: подменяет `Dispatchers.Main` на время каждого теста
 * и снимает все моки после него.
 */
abstract class BaseUnitTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Планировщик подменённого `Dispatchers.Main`: им двигают `delay` во `viewModelScope`. */
    protected val testScheduler: TestCoroutineScheduler get() = mainDispatcherRule.dispatcher.scheduler

    @After
    fun unmockkAllAfterTest() {
        unmockkAll()
    }
}
