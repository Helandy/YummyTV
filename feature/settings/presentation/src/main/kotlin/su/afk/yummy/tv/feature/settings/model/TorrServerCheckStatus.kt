package su.afk.yummy.tv.feature.settings.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult

/** Состояние кнопки «Проверить TorrServer»: ничего не проверяли, идёт запрос или есть итог. */
@Immutable
sealed interface TorrServerCheckStatus {
    data object Idle : TorrServerCheckStatus
    data object Checking : TorrServerCheckStatus
    data class Done(val result: TorrServerCheckResult) : TorrServerCheckStatus
}
