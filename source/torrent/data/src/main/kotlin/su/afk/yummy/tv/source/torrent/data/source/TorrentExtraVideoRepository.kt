package su.afk.yummy.tv.source.torrent.data.source

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogCenter
import su.afk.yummy.tv.core.designsystem.dialog.ChoiceDialogState
import kotlinx.coroutines.flow.MutableStateFlow
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.torrent.data.utils.isTorrentSourceUrl
import su.afk.yummy.tv.source.torrent.data.utils.parseTorrentSourceUrl
import su.afk.yummy.tv.source.torrent.data.utils.searchQueries
import su.afk.yummy.tv.source.torrent.data.utils.toSourceUrl
import javax.inject.Inject

/**
 * Добавляет озвучку «Поиск торрента» для каждой серии, если торренты включены и настроены. Ссылки на файл здесь
 * нет: пользователь выбирает раздачу в [confirmOpen] (до перехода в плеер), поток строит `TorrentExtraStreamRepository`.
 */
internal class TorrentExtraVideoRepository @Inject constructor(
    private val settingsStore: TorrentSettingsStore,
    private val picker: TorrentPicker,
    private val selections: TorrentSelectionHolder,
    private val messages: TorrentMessages,
) : ExtraVideoRepository {

    override val sourceId: String = "torrent"

    // Собственная озвучка, не привязанная к озвучкам Yani; серию при совпадении не перебивает чужие источники.
    override val requiresMatchingDubbing: Boolean = false
    override val priority: Int = -10

    /** Включение/настройка торрентов меняет выдачу сразу, а не после 5-минутного TTL кэша. */
    override suspend fun cacheKeySuffix(): String =
        if (settingsStore.torrentSettings.first().isConfigured) ":on" else ":off"

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        if (!settingsStore.torrentSettings.first().isConfigured) return emptyList()
        val queries = details.searchQueries()
        if (queries.isEmpty()) return emptyList()
        val total = (details.episodes?.aired?.takeIf { it > 0 } ?: details.episodes?.count ?: 1)
            .coerceIn(1, MAX_EPISODES)
        val dubbing = messages.sourceName
        return (1..total).map { number ->
            val episode = number.toString()
            val url = TorrentTarget(details.id, episode, queries).toSourceUrl()
            AnimeVideo(
                // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                episode = episode,
                dubbing = dubbing,
                player = PLAYER_NAME,
                playerId = null,
                iframeUrl = url,
                durationSeconds = null,
            )
        }
    }

    override suspend fun confirmOpen(video: AnimeVideo): Boolean {
        val url = video.iframeUrl
        if (!isTorrentSourceUrl(url)) return true
        val target = parseTorrentSourceUrl(url) ?: return false
        if (!settingsStore.torrentSettings.first().isConfigured) {
            ChoiceDialogCenter.pick(
                title = messages.pickTorrentTitle,
                cancelText = messages.cancel,
                state = MutableStateFlow<ChoiceDialogState>(ChoiceDialogState.Message(messages.notConfigured)),
            )
            return false
        }
        val selection = picker.pick(target) ?: return false
        selections.put(url, selection)
        return true
    }

    private companion object {
        const val PLAYER_NAME = "Torrent"
        const val MAX_EPISODES = 2000
    }
}
