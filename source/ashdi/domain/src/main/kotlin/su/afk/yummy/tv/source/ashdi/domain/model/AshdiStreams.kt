package su.afk.yummy.tv.source.ashdi.domain.model

/**
 * Потоки серии на Ashdi: [masterUrl] — HLS-мастер (адаптивное «Авто»), [qualities] — отдельные варианты «720p» → адрес по
 * возрастанию качества (может быть пусто, если мастер не разобрался).
 */
data class AshdiStreams(
    val qualities: Map<String, String>,
    val masterUrl: String,
)
