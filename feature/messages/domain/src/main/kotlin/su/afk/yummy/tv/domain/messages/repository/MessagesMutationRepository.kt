package su.afk.yummy.tv.domain.messages.repository

import kotlinx.coroutines.flow.StateFlow

/** Счётчик версий мутаций: растёт при каждом изменении, по нему списки понимают, что данные устарели. */
interface MessagesMutationRepository {
    val version: StateFlow<Long>

    fun notifyChanged()
}
