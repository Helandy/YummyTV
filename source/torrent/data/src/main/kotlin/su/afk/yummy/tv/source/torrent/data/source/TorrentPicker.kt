package su.afk.yummy.tv.source.torrent.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogCenter
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogItem
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogState
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import su.afk.yummy.tv.source.torrent.domain.usecase.GetTorrentContentUseCase
import su.afk.yummy.tv.source.torrent.domain.usecase.SearchTorrentsUseCase
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.fileName
import javax.inject.Inject

/**
 * Два шага диалога выбора: раздача из результатов поиска → файл внутри раздачи (добавление в TorrServer идёт
 * под экраном загрузки). `null` — пользователь закрыл диалог или хоста диалогов нет на экране.
 */
internal class TorrentPicker @Inject constructor(
    private val search: SearchTorrentsUseCase,
    private val getContent: GetTorrentContentUseCase,
    private val messages: TorrentMessages,
) {

    suspend fun pick(target: TorrentTarget): TorrentSelection? {
        val torrent = pickTorrent(target) ?: return null
        return pickFile(torrent, target.episode)
    }

    private suspend fun pickTorrent(target: TorrentTarget): TorrentResult? = coroutineScope {
        val state = MutableStateFlow<ChoiceDialogState>(ChoiceDialogState.Loading)
        var results: List<TorrentResult> = emptyList()
        val job = launch {
            state.value = try {
                results = search(target.queries)
                if (results.isEmpty()) {
                    ChoiceDialogState.Message(messages.nothingFound)
                } else {
                    ChoiceDialogState.Items(
                        results.mapIndexed { index, item ->
                            ChoiceDialogItem(
                                id = index.toString(),
                                title = item.title,
                                subtitle = messages.torrentSubtitle(item.sizeBytes, item.seeders, item.tracker),
                            )
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChoiceDialogState.Message(messages.searchFailed)
            }
        }
        val id = ChoiceDialogCenter.pick(messages.pickTorrentTitle, messages.cancel, state)
        job.cancel()
        id?.toIntOrNull()?.let(results::getOrNull)
    }

    private suspend fun pickFile(torrent: TorrentResult, episode: String): TorrentSelection? = coroutineScope {
        val state = MutableStateFlow<ChoiceDialogState>(ChoiceDialogState.Loading)
        var content: TorrentContent? = null
        val job = launch {
            state.value = try {
                val loaded = getContent(torrent.link, episode)
                content = loaded
                if (loaded.files.isEmpty()) {
                    ChoiceDialogState.Message(messages.noVideoFiles)
                } else {
                    ChoiceDialogState.Items(
                        loaded.files.map { file ->
                            ChoiceDialogItem(
                                id = file.index.toString(),
                                title = file.fileName,
                                subtitle = messages.fileSubtitle(file.sizeBytes),
                            )
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChoiceDialogState.Message(messages.serverFailed)
            }
        }
        val id = ChoiceDialogCenter.pick(messages.pickFileTitle, messages.cancel, state)
        job.cancel()
        val loaded = content ?: return@coroutineScope null
        val file = loaded.files.firstOrNull { it.index.toString() == id } ?: return@coroutineScope null
        TorrentSelection(hash = loaded.hash, title = loaded.title.ifBlank { torrent.title }, file = file)
    }
}
