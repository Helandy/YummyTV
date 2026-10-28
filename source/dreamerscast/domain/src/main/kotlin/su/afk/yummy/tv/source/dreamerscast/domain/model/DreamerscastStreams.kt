package su.afk.yummy.tv.source.dreamerscast.domain.model

/**
 * Потоки серии: [masterUrl] — HLS-мастер (адаптивное «Авто»), [qualities] — отдельные варианты «720p» → адрес по возрастанию
 * качества (может быть пусто, если мастер не разобрался).
 */
data class DreamerscastStreams(
    val qualities: Map<String, String>,
    val masterUrl: String,
)
