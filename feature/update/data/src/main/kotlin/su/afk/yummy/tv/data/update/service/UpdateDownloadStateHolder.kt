package su.afk.yummy.tv.data.update.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import javax.inject.Inject
import javax.inject.Singleton

/** Состояние текущей загрузки обновления: сервис пишет сюда, репозиторий отдаёт наружу. */
@Singleton
internal class UpdateDownloadStateHolder @Inject constructor() {

    private data class Entry(val url: String, val state: UpdateDownloadState)

    private val current = MutableStateFlow<Entry?>(null)

    fun update(url: String, state: UpdateDownloadState) {
        current.value = Entry(url, state)
    }

    /** Состояние только для [url]: результат загрузки другой версии не должен подхватываться. */
    fun observe(url: String): Flow<UpdateDownloadState> =
        current.map { entry -> entry?.takeIf { it.url == url }?.state ?: UpdateDownloadState.Idle }
}
