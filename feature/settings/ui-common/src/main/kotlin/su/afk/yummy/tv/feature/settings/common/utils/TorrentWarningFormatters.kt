package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentIndexerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.feature.settings.model.TorrServerCheckStatus
import su.afk.yummy.tv.feature.settings.presentation.R

/**
 * Предупреждение, почему источник торрентов не появится в выборе озвучки; `null` — всё готово.
 * Недоступными TorrServer и индексаторы считаем только по итогу ручной проверки: без неё не знаем.
 * Индексаторы предупреждают, только когда не отвечают все проверенные: хватит одного рабочего.
 */
@Composable
fun TorrentSettings.warningText(checkStatus: TorrServerCheckStatus): String? = when {
    !enabled -> null
    torrServerUrl.isBlank() -> stringResource(R.string.settings_torrent_warning_no_server)
    checkStatus is TorrServerCheckStatus.Done && checkStatus.result !is TorrServerCheckResult.Ok ->
        stringResource(R.string.settings_torrent_warning_server_unavailable)
    activeIndexers.isEmpty() -> stringResource(R.string.settings_torrent_warning_no_indexer)
    checkStatus is TorrServerCheckStatus.Done && checkStatus.indexers.isNotEmpty() &&
        checkStatus.indexers.values.none { it is TorrentIndexerCheckResult.Ok } ->
        stringResource(R.string.settings_torrent_warning_indexer_unavailable)
    else -> null
}
