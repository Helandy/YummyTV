package su.afk.yummy.tv.feature.library.model

/**
 * Удаление из Библиотеки ТВ, ждущее подтверждения в диалоге.
 *
 * [proceed] — то, что сетка сделала бы сразу: перенос фокуса на соседнюю карточку и событие
 * удаления. Вызывается только после «Удалить», поэтому фокус не прыгает, пока пользователь думает.
 */
internal data class PendingLibraryTvRemoval(
    val title: String,
    val listTitle: String,
    val proceed: () -> Unit,
)
