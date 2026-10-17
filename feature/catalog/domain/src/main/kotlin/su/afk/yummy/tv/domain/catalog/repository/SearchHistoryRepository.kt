package su.afk.yummy.tv.domain.catalog.repository

import kotlinx.coroutines.flow.Flow

/** Локальная история поисковых запросов каталога, от новых к старым. */
interface SearchHistoryRepository {
    fun observe(): Flow<List<String>>

    /** Атомарно заменяет историю результатом [transform] от текущего списка. */
    suspend fun update(transform: (List<String>) -> List<String>)

    suspend fun clear()
}
