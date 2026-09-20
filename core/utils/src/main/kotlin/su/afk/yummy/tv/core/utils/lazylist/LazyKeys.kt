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
