package su.afk.yummy.tv.domain.catalog.model

/**
 * Страница выдачи из локального кэша.
 *
 * @param isFresh `false`, если срок жизни кэша истёк: страницу можно показать сразу,
 * но её нужно обновить из сети.
 */
data class CachedSearchPage(
    val page: SearchPage,
    val isFresh: Boolean,
)
