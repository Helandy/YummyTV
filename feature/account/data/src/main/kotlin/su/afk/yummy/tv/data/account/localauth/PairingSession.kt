package su.afk.yummy.tv.data.account.localauth

import su.afk.yummy.tv.data.account.utils.LocalAuthCrypto
import java.util.concurrent.atomic.AtomicInteger

/**
 * Политика одного сеанса сопряжения: срок жизни PIN и лимит неудачных попыток.
 * Про сеть и Android ничего не знает — логику можно проверить обычным unit-тестом.
 */
internal class PairingSession(
    val pin: String = LocalAuthCrypto.generatePin(),
    ttlMs: Long = DEFAULT_TTL_MS,
    maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val deadline = now() + ttlMs

    // CIO обрабатывает запросы конкурентно — счётчик должен быть атомарным.
    private val attempts = AtomicInteger(maxAttempts)

    val attemptsLeft: Int get() = attempts.get()

    /** @return причину отказа, если запрос принимать уже нельзя. */
    fun rejection(): Rejection? = when {
        now() > deadline -> Rejection.EXPIRED
        attempts.get() <= 0 -> Rejection.EXHAUSTED
        else -> null
    }

    /** @return сколько попыток осталось после неудачной. Ниже нуля счётчик не опускается. */
    fun registerFailure(): Int = attempts.updateAndGet { left -> (left - 1).coerceAtLeast(0) }

    enum class Rejection { EXPIRED, EXHAUSTED }

    private companion object {
        const val DEFAULT_TTL_MS = 3 * 60 * 1000L
        const val DEFAULT_MAX_ATTEMPTS = 5
    }
}
