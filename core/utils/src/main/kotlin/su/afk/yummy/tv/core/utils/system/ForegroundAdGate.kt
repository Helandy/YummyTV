package su.afk.yummy.tv.core.utils.system

import java.util.concurrent.atomic.AtomicInteger

/**
 * Отмечает, что поверх приложения показывается полноэкранная реклама в отдельной Activity.
 * Хост-Activity при этом получает `ON_STOP`, хотя пользователь из приложения не уходил.
 */
object ForegroundAdGate {

    private val active = AtomicInteger(0)

    val isShowing: Boolean get() = active.get() > 0

    fun enter() {
        active.incrementAndGet()
    }

    fun exit() {
        active.updateAndGet { if (it > 0) it - 1 else 0 }
    }

    /** Выполняет [block], пока реклама считается показанной. */
    inline fun <T> around(block: () -> T): T {
        enter()
        return try {
            block()
        } finally {
            exit()
        }
    }
}
