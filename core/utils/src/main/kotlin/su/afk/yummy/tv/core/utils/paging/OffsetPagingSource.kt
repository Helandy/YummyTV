package su.afk.yummy.tv.core.utils.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching

/*
 * Дженерик-обвязка над Paging 3 для offset-пагинации.
 *
 * Тип элемента страницы вынесен в параметр `T`, поэтому один и тот же код работает для любых
 * доменных моделей (обзоры, комментарии, диалоги и т.д.) — вызывающему коду достаточно передать
 * доменную загрузку `(limit, offset) -> List<T>`. Граница `T : Any` требуется самим Paging:
 * [PagingSource]/[PagingData] не допускают nullable-элементы.
 *
 * Точки входа:
 *  - [pagingFlow] — вернуть готовый `Flow<PagingData<T>>` (когда перезагрузка не нужна);
 *  - [pagingSource] — то же + [PagedSource.invalidate] для ручного сброса (мутации/сорт/фильтр).
 */

const val DEFAULT_PAGE_SIZE = 20

/**
 * Результат одной подгрузки страницы.
 *
 * @param items       элементы страницы.
 * @param nextOffset  offset следующей страницы (обычно `offset + items.size`).
 * @param canLoadMore есть ли ещё данные (обычно `items.size >= limit`).
 */
data class OffsetPage<T>(
    val items: List<T>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)

/**
 * Держатель [Flow] со страницами и текущего [PagingSource] для инвалидации.
 * Использовать, когда список нужно перезагружать по внешнему событию (мутации и т.п.).
 */
class PagedSource<T : Any>(
    val flow: Flow<PagingData<T>>,
    private val onInvalidate: () -> Unit,
) {
    fun invalidate() = onInvalidate()
}

/**
 * Собирает `Pager { OffsetPagingSource { … } }.flow.cachedIn(scope)` из простой доменной загрузки.
 * `nextOffset`/`canLoadMore` считаются автоматически: страница считается последней, когда вернулось
 * меньше [pageSize] элементов. Возвращает [PagedSource] с доступом к [PagedSource.invalidate].
 *
 * Если сервер сам отдаёт `nextOffset`/`canLoadMore` (курсорная пагинация) — этот хелпер не подходит,
 * используйте [OffsetPagingSource] напрямую и стройте [OffsetPage] вручную.
 *
 * ```
 * private var paged: PagedSource<Comment>? = null
 * fun comments() = pagingSource(viewModelScope) { limit, offset -> loadComments(limit, offset) }
 *     .also { paged = it }.flow
 * // при мутации: paged?.invalidate()
 * ```
 *
 * @param scope         скоуп для `cachedIn` (обычно `viewModelScope`).
 * @param pageSize      размер страницы (он же `initialLoadSize`).
 * @param initialOffset offset первой страницы.
 * @param itemKey       уникальный ID для удаления повторов внутри и между страницами.
 * @param load          доменная загрузка `(limit, offset) -> List<T>`.
 */
fun <T : Any> pagingSource(
    scope: CoroutineScope,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialOffset: Int = 0,
    itemKey: ((T) -> Any)? = null,
    load: suspend (limit: Int, offset: Int) -> List<T>,
): PagedSource<T> {
    var current: PagingSource<Int, T>? = null
    val flow = Pager(
        PagingConfig(pageSize = pageSize, initialLoadSize = pageSize, enablePlaceholders = false),
    ) {
        OffsetPagingSource(initialOffset, itemKey) { limit, offset ->
            val items = load(limit, offset)
            OffsetPage(items, offset + items.size, items.size >= limit)
        }.also { current = it }
    }.flow.cachedIn(scope)
    return PagedSource(flow) { current?.invalidate() }
}

/**
 * Упрощённый вариант [pagingSource], когда инвалидация не нужна — возвращает только [Flow].
 *
 * ```
 * fun history() = pagingFlow(viewModelScope, pageSize = 100) { limit, offset ->
 *     getWatchHistoryPage(limit, offset)
 * }
 * ```
 */
fun <T : Any> pagingFlow(
    scope: CoroutineScope,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialOffset: Int = 0,
    itemKey: ((T) -> Any)? = null,
    load: suspend (limit: Int, offset: Int) -> List<T>,
): Flow<PagingData<T>> = pagingSource(scope, pageSize, initialOffset, itemKey, load).flow

/**
 * Дженерик [PagingSource] с offset-ключами. Низкоуровневый строительный блок — предпочитайте
 * [pagingFlow]/[pagingSource]; используйте напрямую, когда нужен кастомный [OffsetPage]
 * (серверные `nextOffset`/`canLoadMore`, дедупликация и т.п.).
 *
 * Пустая, но непоследняя страница (`items.isEmpty() && canLoadMore`) не завершает пагинацию —
 * источник перескакивает на `nextOffset` и грузит дальше, чтобы отфильтрованные страницы не
 * обрывали список.
 *
 * @param initialOffset offset первой страницы (и нижняя граница refresh-ключа).
 * @param itemKey       уникальный ID; повторы удаляются в пределах одного источника,
 *                      серверные offset и признак конца страницы сохраняются.
 * @param loadPage      загрузка страницы `(limit, offset) -> OffsetPage<T>`.
 */
class OffsetPagingSource<T : Any>(
    private val initialOffset: Int = 0,
    private val itemKey: ((T) -> Any)? = null,
    private val loadPage: suspend (limit: Int, offset: Int) -> OffsetPage<T>,
) : PagingSource<Int, T>() {

    private val seenKeys = mutableSetOf<Any>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        runSuspendCatching {
            val offset = params.key ?: initialOffset
            val loadedKeys = mutableSetOf<Any>()
            val previousKeys = if (params is LoadParams.Refresh) emptySet() else seenKeys
            suspend fun loadUniquePage(pageOffset: Int): OffsetPage<T> {
                val page = loadPage(params.loadSize, pageOffset)
                val keyOf = itemKey ?: return page
                return page.copy(
                    items = page.items.filter { item ->
                        val key = keyOf(item)
                        key !in previousKeys && loadedKeys.add(key)
                    },
                )
            }
            var currentOffset = offset
            var page = loadUniquePage(currentOffset)

            while (page.items.isEmpty() && page.canLoadMore && page.nextOffset > currentOffset) {
                currentOffset = page.nextOffset
                page = loadUniquePage(currentOffset)
            }

            // Commit keys only after the entire load succeeds, so retry cannot lose items.
            if (params is LoadParams.Refresh) seenKeys.clear()
            seenKeys.addAll(loadedKeys)
            LoadResult.Page(
                data = page.items,
                prevKey = null,
                nextKey = if (page.canLoadMore && page.nextOffset > currentOffset) {
                    page.nextOffset
                } else {
                    null
                },
            )
        }.getOrElse { error -> LoadResult.Error(error) }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            (anchorPosition - state.config.initialLoadSize / 2).coerceAtLeast(initialOffset)
        }
}
