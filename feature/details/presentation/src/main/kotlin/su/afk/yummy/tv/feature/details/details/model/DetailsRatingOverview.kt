package su.afk.yummy.tv.feature.details.details.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Сводка рейтинга и статистики списков для карточки на экране деталей. */
@Immutable
data class DetailsRatingOverview(
    val average: Double? = null,
    val votes: Int = 0,
    /** Число голосов за оценки 1..10, индекс = оценка − 1. */
    val distribution: ImmutableList<Int> = persistentListOf(),
    /** Списки с ненулевым числом пользователей, в порядке «Смотрю → Брошено». */
    val listCounts: ImmutableList<DetailsListCount> = persistentListOf(),
) {
    val isEmpty: Boolean get() = votes <= 0 && listCounts.isEmpty()
}
