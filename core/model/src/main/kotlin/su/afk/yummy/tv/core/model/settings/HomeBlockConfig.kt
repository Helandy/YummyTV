package su.afk.yummy.tv.core.model.settings

/** Настройка одного блока главной: [enabled] — показывать ли его; позиция в списке задаёт порядок. */
data class HomeBlockConfig(
    val kind: HomeBlockKind,
    val enabled: Boolean = true,
) {
    companion object {
        val default: List<HomeBlockConfig> = HomeBlockKind.entries.map { HomeBlockConfig(it) }
    }
}
