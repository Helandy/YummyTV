package su.afk.yummy.tv.domain.update.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState

interface UpdateDownloadRepository {
    /** Запускает фоновую загрузку [url]; уже идущая загрузка не перезапускается. */
    fun start(url: String)

    /** Состояние загрузки именно этого [url], чтобы не подхватить результат старой версии. */
    fun observe(url: String): Flow<UpdateDownloadState>
}
