package su.afk.yummy.tv.core.utils.coroutines

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Аналог [runCatching] для suspend-кода: отмену текущей корутины пробрасывает дальше,
 * всё остальное (включая чужую CancellationException, например таймаут внутри блока)
 * отдаёт в [Result.failure].
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: Throwable) {
        currentCoroutineContext().ensureActive()
        Result.failure(error)
    }
