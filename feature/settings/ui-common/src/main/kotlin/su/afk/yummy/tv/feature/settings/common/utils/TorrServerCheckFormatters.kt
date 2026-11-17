package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentIndexerCheckResult
import su.afk.yummy.tv.feature.settings.model.TorrServerCheckStatus
import su.afk.yummy.tv.feature.settings.presentation.R

/**
 * Подпись под кнопкой «Проверить подключение»: пусто, пока не проверяли; иначе ход или итог — строка про TorrServer
 * и по строке на каждый проверенный индексатор.
 */
@Composable
fun TorrServerCheckStatus.hintText(): String = when (this) {
    TorrServerCheckStatus.Idle -> ""
    TorrServerCheckStatus.Checking -> stringResource(R.string.settings_torrent_check_checking)
    is TorrServerCheckStatus.Done -> {
        val server = when (val result = result) {
            is TorrServerCheckResult.Ok ->
                stringResource(R.string.settings_torrent_check_ok, result.version.ifBlank { "—" })
            TorrServerCheckResult.Unreachable -> stringResource(R.string.settings_torrent_check_unreachable)
            TorrServerCheckResult.Unauthorized -> stringResource(R.string.settings_torrent_check_unauthorized)
            is TorrServerCheckResult.UnexpectedResponse ->
                stringResource(R.string.settings_torrent_check_unexpected, result.httpCode)
        }
        (listOf(server) + indexers.map { (indexer, result) -> indexerLine(indexer, result) }).joinToString("\n")
    }
}

@Composable
private fun indexerLine(indexer: TorrentIndexer, result: TorrentIndexerCheckResult): String {
    val name = when (indexer) {
        TorrentIndexer.JACRED -> "JacRed"
        TorrentIndexer.JACKETT -> "Jackett"
    }
    return when (result) {
        TorrentIndexerCheckResult.Ok -> stringResource(R.string.settings_torrent_check_indexer_ok, name)
        TorrentIndexerCheckResult.Unreachable -> stringResource(R.string.settings_torrent_check_indexer_unreachable, name)
        TorrentIndexerCheckResult.Unauthorized -> stringResource(R.string.settings_torrent_check_indexer_unauthorized, name)
        is TorrentIndexerCheckResult.UnexpectedResponse ->
            stringResource(R.string.settings_torrent_check_indexer_unexpected, name, result.httpCode)
    }
}
