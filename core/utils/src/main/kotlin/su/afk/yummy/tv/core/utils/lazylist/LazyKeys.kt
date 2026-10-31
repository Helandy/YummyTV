package su.afk.yummy.tv.core.utils.lazylist

/**
 * Ключ элемента lazy-списка с префиксом секции.
 *
 * Compose падает с `IllegalArgumentException: Key "..." was already used`, если в одном
 * lazy-layout два элемента получили одинаковый ключ. Сырой числовой id опасен сразу по двум
 * причинам: он может совпасть с id из соседней секции того же `LazyColumn` и он живёт в одном
 * пространстве с индексами-плейсхолдерами (`items[index]?.id ?: index`).
 *
 * Префикс разводит секции, а `null`-id (плейсхолдер пагинации) уходит в отдельное пространство.
 *
 * Ключ обязан быть saveable-типом (String — годится), поэтому `id` приводится к строке.
 *
 * @param index нужен только для плейсхолдеров пагинации; у списков без них его можно не указывать.
 */
fun lazyKey(prefix: String, id: Any?, index: Int = -1): String =
    if (id == null) "$prefix:placeholder:$index" else "$prefix:$id"

/**
 * Ключ элемента lazy-списка поверх Paging, который не бросает при устаревшем индексе.
 *
 * Lazy-layout строит карту ключей по `count`, прочитанному в композиции, а Paging за это время мог
 * подменить снапшот (новый запрос, рефреш) на более короткий. `LazyPagingItems.peek(index)` на
 * индексе за пределами снапшота бросает `IndexOutOfBoundsException`, поэтому индекс сверяется с
 * актуальным размером. Для плейсхолдера и устаревшего индекса ключ берётся из отдельного
 * пространства [lazyKey], где он не пересечётся с ключами настоящих элементов.
 *
 * @param itemCount актуальный размер снапшота, а не значение, закэшированное в композиции.
 * @param peek чтение элемента без запуска подгрузки.
 */
inline fun <T : Any> pagingItemKey(
    index: Int,
    itemCount: Int,
    peek: (Int) -> T?,
    placeholderPrefix: String,
    key: (T) -> Any,
): Any {
    val item = if (index in 0 until itemCount) peek(index) else null
    return if (item == null) lazyKey(placeholderPrefix, null, index) else key(item)
}
