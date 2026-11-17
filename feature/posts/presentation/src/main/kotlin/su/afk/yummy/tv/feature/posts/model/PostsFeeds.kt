package su.afk.yummy.tv.feature.posts.model

import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.model.PostSummary

/**
 * Ленты новостей по паре (категория, сортировка).
 *
 * Каждая лента создаётся один раз и держит загруженные страницы (`cachedIn`), поэтому возврат в уже
 * открытую категорию показывает список сразу, а у каждой страницы пейджера может быть свой список.
 */
@Stable
class PostsFeeds(
    private val scope: CoroutineScope,
    private val load: suspend (category: String?, sort: PostSort, limit: Int, offset: Int) -> List<PostSummary>,
) {
    private val feeds = mutableMapOf<Pair<String?, PostSort>, Flow<PagingData<PostSummary>>>()

    operator fun get(category: String?, sort: PostSort): Flow<PagingData<PostSummary>> =
        feeds.getOrPut(category to sort) {
            pagingFlow(scope, itemKey = { it.id }) { limit, offset -> load(category, sort, limit, offset) }
        }
}
