package su.afk.yummy.tv.domain.library.model

/** Ошибка конкретного сетевого этапа импорта или экспорта списка с прогрессом по пачкам. */
class AnimeListTransferStageException(
    val stage: AnimeListTransferStage,
    override val cause: Throwable,
    val completedBatches: Int = 0,
    val totalBatches: Int = 1,
) : RuntimeException(cause.message, cause)
