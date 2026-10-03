package su.afk.yummy.tv.domain.collection.repository

import kotlinx.coroutines.flow.StateFlow

/** Счётчик версий мутаций: растёт при каждом изменении, по нему списки понимают, что данные устарели. */
interface CollectionMutationRepository {
    val version: StateFlow<Long>

    fun notifyChanged()
}
