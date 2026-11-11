package su.afk.yummy.tv.source.torrent.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        if (!settingsStore.torrentSettings.first().isConfigured) {
            return PlayerStreamResolveResult.Unavailable(messages.notConfigured)
        }
        val target = parseTorrentSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        val selection = selections.get(url)
            ?: picker.pick(target)?.also { selections.put(url, it) }
            ?: return PlayerStreamResolveResult.Unavailable(messages.cancelled)
        return try {
            val stream = buildStream(selection)
            PlayerStreamResolveResult.Stream(url = stream.url, headers = stream.headers)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlayerStreamResolveResult.Failed("torrserver: ${e.javaClass.simpleName}")
        }
    }
}
