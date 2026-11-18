package su.afk.yummy.tv.domain.library.model

/**
 * Результат пакетного сопоставления внешних ID с каталогом YummyAnime:
 * [resolvedByExternalId] — найденные кандидаты, [failedExternalIds] — ID из пачек, не загруженных
 * из-за сетевой или серверной ошибки (чтобы не путать их с отсутствующими в каталоге тайтлами).
 */
data class ExternalIdLookupResult(
    val resolvedByExternalId: Map<Int, List<ResolvedExternalAnime>>,
    val failedExternalIds: Set<Int> = emptySet(),
)
