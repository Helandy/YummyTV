package su.afk.yummy.tv.domain.home.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

interface RemoteContinueWatchingRepository {

    /**
     * Серверная история просмотра текущего пользователя, по записи на тайтл, включая досмотренные.
     * Гостю — пустой список; при смене пользователя список сразу сбрасывается.
     */
    fun observeLastWatches(): Flow<List<HomeContinueWatchingItem>>

    /** Просит обновить историю с сервера; результат придёт в [observeLastWatches]. */
    fun requestRefresh()
}
