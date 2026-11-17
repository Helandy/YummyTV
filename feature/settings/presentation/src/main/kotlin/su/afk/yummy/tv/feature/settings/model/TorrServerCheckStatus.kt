package su.afk.yummy.tv.feature.settings.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentIndexerCheckResult

/** Состояние кнопки «Проверить подключение»: ничего не проверяли, идёт запрос или есть итог по серверу и индексаторам. */
@Immutable
sealed interface TorrServerCheckStatus {
    data object Idle : TorrServerCheckStatus
    data object Checking : TorrServerCheckStatus
    data class Done(
        val result: TorrServerCheckResult,
        /** Итоги по активным индексаторам; пусто, пока их не проверяли. */
        val indexers: Map<TorrentIndexer, TorrentIndexerCheckResult> = emptyMap(),
    ) : TorrServerCheckStatus
}
