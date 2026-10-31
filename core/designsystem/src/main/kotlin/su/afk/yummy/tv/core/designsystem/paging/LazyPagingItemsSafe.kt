package su.afk.yummy.tv.core.designsystem.paging

import androidx.paging.compose.LazyPagingItems
import su.afk.yummy.tv.core.utils.lazylist.pagingItemKey

/**
 * Замена `itemKey` из paging-compose, устойчивая к устаревшему индексу: см. [pagingItemKey].
 *
 * @param placeholderPrefix префикс ключей плейсхолдеров; у списков в одном layout должен различаться.
 */
fun <T : Any> LazyPagingItems<T>.safeItemKey(
    placeholderPrefix: String = "paging",
    key: (T) -> Any,
): (Int) -> Any = { index ->
    pagingItemKey(index, itemCount, peek = ::peek, placeholderPrefix = placeholderPrefix, key = key)
}

/**
 * Аналог `get(index)`, который возвращает `null` вместо `IndexOutOfBoundsException`, если снапшот
 * Paging успел стать короче, чем `count` lazy-layout. Подгрузку страниц запускает так же, как `get`.
 */
fun <T : Any> LazyPagingItems<T>.getOrNull(index: Int): T? =
    if (index in 0 until itemCount) get(index) else null
