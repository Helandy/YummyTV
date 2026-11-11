package su.afk.yummy.tv.source.torrent.domain.usecase

import su.afk.yummy.tv.core.model.settings.TorrentSort
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.model.sortedFor
import su.afk.yummy.tv.source.torrent.domain.repository.TorrentIndexerRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Ищет раздачи по нескольким названиям тайтла (русское и латинское): запросы идут по очереди, результаты
 * объединяются без повторов по ссылке и сортируются по настройке пользователя ([TorrentSort], порядок можно перевернуть). Если упали все запросы, исключение
 * последнего пробрасывается; если часть отработала, ошибки остальных игнорируются.
 */
class SearchTorrentsUseCase @Inject constructor(
    private val indexer: TorrentIndexerRepository,
) {
    suspend operator fun invoke(
        queries: List<String>,
        sort: TorrentSort = TorrentSort.SEEDERS,
        reversed: Boolean = false,
    ): List<TorrentResult> {
        val distinctQueries = queries.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        var lastError: Throwable? = null
        var anySucceeded = false
        val merged = LinkedHashMap<String, TorrentResult>()
        for (query in distinctQueries) {
            try {
                indexer.search(query).forEach { merged.putIfAbsent(it.link, it) }
                anySucceeded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (!anySucceeded) lastError?.let { throw it }
        return merged.values.toList().sortedFor(sort, reversed)
    }
}
