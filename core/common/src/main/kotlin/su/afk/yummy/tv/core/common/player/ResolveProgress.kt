package su.afk.yummy.tv.core.common.player

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Приёмник шагов и подробностей получения потока, лежащий в контексте корутины резолва. Сетевой слой и
 * источники читают его из контекста и сообщают прогресс, не меняя сигнатур своих контрактов.
 *
 * @param emitsSteps false у экстракторов, которые сами отдают точные шаги: сетевой слой тогда сообщает
 *   только подробности запросов и не двигает шаг.
 */
class ResolveProgress(
    private val onStatus: (PlayerStreamResolveStatus) -> Unit,
    private val onDetail: (ResolveDetail) -> Unit = {},
    val emitsSteps: Boolean = true,
) : AbstractCoroutineContextElement(Key) {

    fun report(status: PlayerStreamResolveStatus) = onStatus(status)

    fun reportDetail(detail: ResolveDetail) = onDetail(detail)

    companion object Key : CoroutineContext.Key<ResolveProgress>
}
