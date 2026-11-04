package su.afk.yummy.tv.core.common.player

import kotlin.coroutines.coroutineContext

/** Сообщает шаг получения потока, если вызов идёт внутри резолва; иначе ничего не делает. */
suspend fun reportResolveStep(status: PlayerStreamResolveStatus) {
    coroutineContext[ResolveProgress]?.report(status)
}
