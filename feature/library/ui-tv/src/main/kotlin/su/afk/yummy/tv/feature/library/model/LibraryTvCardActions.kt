package su.afk.yummy.tv.feature.library.model

/**
 * Действия карточки, открытые долгим нажатием в сетке библиотеки ТВ.
 *
 * [open] и [remove] замкнуты на сетку, потому что только она знает, куда вернуть фокус.
 */
internal data class LibraryTvCardActions(
    val title: String,
    val open: () -> Unit,
    val remove: () -> Unit,
)
