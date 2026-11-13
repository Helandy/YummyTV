package su.afk.yummy.tv.core.model.settings

/** Настройка одного блока деталей: [enabled] — показывать ли его; позиция в списке задаёт порядок. */
data class DetailsSectionConfig(
    val kind: DetailsSectionKind,
    val enabled: Boolean = true,
) {
    companion object {
        val default: List<DetailsSectionConfig> = DetailsSectionKind.entries.map { DetailsSectionConfig(it) }
    }
}
