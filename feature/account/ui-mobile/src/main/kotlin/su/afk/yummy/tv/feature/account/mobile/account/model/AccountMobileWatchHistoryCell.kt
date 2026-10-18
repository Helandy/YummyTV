package su.afk.yummy.tv.feature.account.mobile.account.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Один день календаря активности: [intensity] 0 — день без просмотров, иначе 0.32..1.
 * [isFuture] — оставшиеся дни текущей недели, их рисуем пустыми и не даём выбрать.
 */
@Immutable
internal data class AccountMobileWatchHistoryCell(
    val date: LocalDate,
    val durationSeconds: Long,
    val episodeCount: Int,
    val intensity: Float,
    val isToday: Boolean,
    val isFuture: Boolean,
)
