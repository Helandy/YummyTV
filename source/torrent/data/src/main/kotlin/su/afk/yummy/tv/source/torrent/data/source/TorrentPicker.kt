package su.afk.yummy.tv.source.torrent.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogCenter
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogItem
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogState
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogStat
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogTone
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.source.torrent.data.utils.pinServerTorrentsFirst
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentMetadataUnavailableException
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import su.afk.yummy.tv.source.torrent.domain.model.autoSelectFor
import su.afk.yummy.tv.source.torrent.domain.model.fileName
import su.afk.yummy.tv.source.torrent.domain.model.infoHash
import su.afk.yummy.tv.source.torrent.domain.model.parseTorrentTags
import su.afk.yummy.tv.source.torrent.domain.usecase.GetKnownTorrentHashesUseCase
import su.afk.yummy.tv.source.torrent.domain.usecase.GetTorrentContentUseCase
import su.afk.yummy.tv.source.torrent.domain.usecase.SearchTorrentsUseCase
import javax.inject.Inject

/**
 * Два шага диалога выбора: раздача из результатов поиска → файл внутри раздачи (добавление в TorrServer идёт
 * под экраном загрузки). При ошибке поиска или TorrServer диалог предлагает «Повторить» и перезапускает шаг.
 * `null` — пользователь закрыл диалог или хоста диалогов нет на экране.
 */
internal class TorrentPicker @Inject constructor(
    private val search: SearchTorrentsUseCase,
    private val getContent: GetTorrentContentUseCase,
    private val getKnownHashes: GetKnownTorrentHashesUseCase,
    private val messages: TorrentMessages,
    private val settingsStore: TorrentSettingsStore,
    private val selections: TorrentSelectionHolder,
    private val decoders: VideoDecoderSupport,
) {

    /**
     * [reuseTorrent] — переход на соседнюю серию внутри торрента: раздача не спрашивается заново, остаётся выбрать файл
     * нужной серии (обычно автоматически).
     */
    suspend fun pick(target: TorrentTarget, reuseTorrent: Boolean = false): TorrentSelection? {
        val remembered = if (reuseTorrent) selections.torrentFor(target.animeId) else null
        val torrent = remembered ?: pickTorrent(target)?.also { selections.rememberTorrent(target.animeId, it) }
            ?: return null
        return pickFile(torrent, target.episode)
    }

    private suspend fun pickTorrent(target: TorrentTarget): TorrentResult? {
        while (true) {
            val outcome = pickTorrentOnce(target)
            if (outcome != RETRY) return outcome.takeIf { it !== NO_CHOICE }?.let { it as TorrentResult }
        }
    }

    private suspend fun pickTorrentOnce(target: TorrentTarget): Any? = coroutineScope {
        reportResolveStep(PlayerStreamResolveStatus.WaitingForTorrent)
        val state = MutableStateFlow<ChoiceDialogState>(ChoiceDialogState.Progress(messages.stepSearching))
        var results: List<TorrentResult> = emptyList()
        val job = launch {
            state.value = try {
                val settings = settingsStore.torrentSettings.first()
                // Список сервера нужен только для метки, поэтому запрашиваем его параллельно с поиском.
                val known = async { getKnownHashes() }
                val found = search(target.queries, settings.sort, settings.sortReversed)
                val knownHashes = known.await()
                // Играющая сейчас раздача и всё, что уже лежит в TorrServer, — в начале списка.
                val currentHash = selections.torrentFor(target.animeId)?.infoHash
                results = found.pinServerTorrentsFirst(currentHash, knownHashes)
                if (results.isEmpty()) {
                    ChoiceDialogState.Message(messages.nothingFound)
                } else {
                    ChoiceDialogState.Items(results.mapIndexed { index, item -> item.toChoiceItem(index, item.infoHash in knownHashes) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChoiceDialogState.Message(messages.searchFailed, retryText = messages.retry)
            }
        }
        val id = ChoiceDialogCenter.pick(messages.pickTorrentTitle, messages.cancel, state)
        job.cancel()
        when (id) {
            ChoiceDialogCenter.RETRY_ID -> RETRY
            null -> NO_CHOICE
            else -> id.toIntOrNull()?.let(results::getOrNull) ?: NO_CHOICE
        }
    }

    private suspend fun pickFile(torrent: TorrentResult, episode: String): TorrentSelection? {
        while (true) {
            val outcome = pickFileOnce(torrent, episode)
            if (outcome != RETRY) return outcome.takeIf { it !== NO_CHOICE }?.let { it as TorrentSelection }
        }
    }

    private suspend fun pickFileOnce(torrent: TorrentResult, episode: String): Any? = coroutineScope {
        reportResolveStep(PlayerStreamResolveStatus.WaitingForTorrentFile)
        val state = MutableStateFlow<ChoiceDialogState>(
            ChoiceDialogState.Progress(messages.stepFetchingFilesPeers(0, 0, torrent.seeders)),
        )
        var content: TorrentContent? = null
        val job = launch {
            state.value = try {
                val loaded = getContent(torrent.link, episode, torrent.title) { progress ->
                    state.value = ChoiceDialogState.Progress(
                        messages.stepFetchingFilesPeers(
                            progress.activePeers,
                            progress.totalPeers,
                            torrent.seeders,
                        ),
                    )
                }
                content = loaded
                val auto = loaded.files.autoSelectFor(episode)
                if (loaded.files.isEmpty()) {
                    ChoiceDialogState.Message(messages.noVideoFiles)
                } else if (auto != null) {
                    // Один видеофайл или один файл с номером серии: пользователя не спрашиваем.
                    ChoiceDialogState.AutoSelect(auto.index.toString())
                } else {
                    ChoiceDialogState.Items(
                        loaded.files.map { file ->
                            ChoiceDialogItem(
                                id = file.index.toString(),
                                title = file.fileName,
                                stats = listOfNotNull(
                                    messages.sizeText(file.sizeBytes).takeIf { it.isNotEmpty() }
                                        ?.let { ChoiceDialogStat(it) },
                                ),
                                badges = parseTorrentTags(file.fileName).toBadges(messages.subtitlesBadge),
                            )
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TorrentMetadataUnavailableException) {
                ChoiceDialogState.Message(messages.noPeers, retryText = messages.retry)
            } catch (e: Exception) {
                ChoiceDialogState.Message(messages.serverFailed, retryText = messages.retry)
            }
        }
        val id = ChoiceDialogCenter.pick(messages.pickFileTitle, messages.cancel, state)
        job.cancel()
        if (id == ChoiceDialogCenter.RETRY_ID) return@coroutineScope RETRY
        val loaded = content ?: return@coroutineScope NO_CHOICE
        val file = loaded.files.firstOrNull { it.index.toString() == id } ?: return@coroutineScope NO_CHOICE
        TorrentSelection(hash = loaded.hash, title = loaded.title.ifBlank { torrent.title }, file = file)
    }

    private fun TorrentResult.toChoiceItem(index: Int, onServer: Boolean): ChoiceDialogItem = ChoiceDialogItem(
        id = index.toString(),
        title = title,
        badges = parseTorrentTags(title).toBadges(messages.subtitlesBadge),
        markers = listOfNotNull(messages.inTorrServerBadge.takeIf { onServer }),
        warnings = listOfNotNull(
            messages.hevcUnsupportedWarning.takeIf { parseTorrentTags(title).codec == "HEVC" && !decoders.supportsHevcMain10() },
        ),
        stats = listOfNotNull(
            messages.sizeText(sizeBytes).takeIf { it.isNotEmpty() }?.let { ChoiceDialogStat(it) },
            ChoiceDialogStat(messages.seedersText(seeders), seedersTone(seeders)),
            tracker.takeIf { it.isNotBlank() }?.let { ChoiceDialogStat(it) },
        ),
    )

    private fun seedersTone(seeders: Int): ChoiceDialogTone = when {
        seeders >= GOOD_SEEDERS -> ChoiceDialogTone.Good
        seeders > 0 -> ChoiceDialogTone.Warning
        else -> ChoiceDialogTone.Bad
    }

    private companion object {
        const val GOOD_SEEDERS = 10

        /** Маркеры исхода шага: повторить / пользователь ничего не выбрал. */
        val RETRY = Any()
        val NO_CHOICE = Any()
    }
}
