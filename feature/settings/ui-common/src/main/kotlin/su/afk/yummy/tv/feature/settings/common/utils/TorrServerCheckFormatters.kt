package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.feature.settings.model.TorrServerCheckStatus
import su.afk.yummy.tv.feature.settings.presentation.R

/** Подпись под кнопкой «Проверить TorrServer»: пусто, пока не проверяли; иначе ход или итог проверки. */
@Composable
fun TorrServerCheckStatus.hintText(): String = when (this) {
    TorrServerCheckStatus.Idle -> ""
    TorrServerCheckStatus.Checking -> stringResource(R.string.settings_torrent_check_checking)
    is TorrServerCheckStatus.Done -> when (val result = result) {
        is TorrServerCheckResult.Ok ->
            stringResource(R.string.settings_torrent_check_ok, result.version.ifBlank { "—" })
        TorrServerCheckResult.Unreachable -> stringResource(R.string.settings_torrent_check_unreachable)
        TorrServerCheckResult.Unauthorized -> stringResource(R.string.settings_torrent_check_unauthorized)
        is TorrServerCheckResult.UnexpectedResponse ->
            stringResource(R.string.settings_torrent_check_unexpected, result.httpCode)
    }
}
