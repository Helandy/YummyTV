package su.afk.yummy.tv.source.torrent.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.torrent.data.utils.isTorrentSourceUrl
import su.afk.yummy.tv.source.torrent.data.utils.parseTorrentSourceUrl
import su.afk.yummy.tv.source.torrent.domain.usecase.BuildTorrentStreamUseCase
import javax.inject.Inject

/**
 * Превращает `source://torrent/…` в адрес потока TorrServer. Выбор раздачи обычно уже сделан в деталке; если его
 * нет (вход через «Продолжить», выбор устарел), диалог показывается здесь.
 */
internal class TorrentExtraStreamRepository @Inject constructor(
    private val settingsStore: TorrentSettingsStore,
    private val selections: TorrentSelectionHolder,
    private val picker: TorrentPicker,
    private val buildStream: BuildTorrentStreamUseCase,
    private val messages: TorrentMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isTorrentSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult = resolve(url, previousUrl = null)

    /** Повторный выбор «Поиск торрента» в плеере: выбор забыт, следующий [resolve] покажет диалог. */
    override fun forgetSelection(url: String): Boolean {
        selections.forget(url)
        return true
    }

    override suspend fun resolve(url: String, previousUrl: String?): PlayerStreamResolveResult {
        if (!settingsStore.torrentSettings.first().isConfigured) {
            return PlayerStreamResolveResult.Unavailable(messages.notConfigured)
        }
        val target = parseTorrentSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        // Возврат с другой озвучки (Kodik и т.п.) — всегда диалог; сохранённый выбор нужен только для повтора после ошибки.
        val fromOtherSource = previousUrl != null && !isTorrentSourceUrl(previousUrl)
        val selection = (if (fromOtherSource) null else selections.get(url))
            ?: picker.pick(target, reuseTorrent = previousUrl.isSiblingTorrentEpisode(target))?.also { selections.put(url, it) }
            ?: return PlayerStreamResolveResult.Unavailable(messages.cancelled)
        reportResolveStep(PlayerStreamResolveStatus.RequestingStream)
        return try {
            // Ждём, пока TorrServer найдёт пиров и начнёт отдавать данные, пока плеер показывает загрузку.
            val stream = buildStream(selection)
                ?: return PlayerStreamResolveResult.Unavailable(messages.streamNotStarting)
            selections.markStarted(url)
            PlayerStreamResolveResult.Stream(url = stream.url, headers = stream.headers, sourceLabel = selection.title)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlayerStreamResolveResult.Failed("torrserver: ${e.javaClass.simpleName}")
        }
    }
}

/** Предыдущее видео плеера — другая серия того же тайтла из торрент-источника: это «следующая/предыдущая серия». */
private fun String?.isSiblingTorrentEpisode(target: TorrentTarget): Boolean {
    val previous = this?.let(::parseTorrentSourceUrl) ?: return false
    return previous.animeId == target.animeId && previous.episode != target.episode
}
