package su.afk.yummy.tv.source.availability.domain.model

/** Тайтл найден у балансера [sourceId]: по [key] и [url] приложение само забирает плейлист, сервер ссылок на потоки не хранит. */
data class SourceAvailability(
    val sourceId: String,
    /** Ключ тайтла у источника (slug, id). */
    val key: String,
    /** Страница тайтла у источника. */
    val url: String,
    val episodesAvailable: Int,
    /** Сколько серий заявлено; `null`, если источник не знает. */
    val episodesTotal: Int?,
    /** Серии у источника ещё прибавляются: список не стоит кэшировать надолго. */
    val ongoing: Boolean,
    val qualities: List<String>,
    /** Варианты озвучки со своими сериями (у AniLibria: озвучка и новый дубляж); пусто, если источник отдаёт серии `1..episodesAvailable`. */
    val variants: List<SourceVariant> = emptyList(),
    /** Адреса той же страницы на зеркалах сайта: пробуются, если [url] не открывается (блокировка). Пусто, если зеркал нет. */
    val mirrors: List<String> = emptyList(),
)
