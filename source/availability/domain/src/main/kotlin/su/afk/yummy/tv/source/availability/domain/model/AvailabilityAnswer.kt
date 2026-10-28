package su.afk.yummy.tv.source.availability.domain.model

/** Ответ сервера: где тайтл найден и какие источники не ответили (по ним ничего не известно). */
data class AvailabilityAnswer(
    val sources: List<SourceAvailability>,
    val failedSourceIds: Set<String>,
)
