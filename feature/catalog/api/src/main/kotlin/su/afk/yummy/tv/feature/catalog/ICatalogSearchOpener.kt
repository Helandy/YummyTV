package su.afk.yummy.tv.feature.catalog

import kotlinx.coroutines.flow.StateFlow

/**
 * Открытие поиска каталога из других фич: переключает на вкладку каталога и просит экран
 * поставить фокус в поле запроса.
 *
 * Запрос хранится, пока экран каталога его не обработает: при первом открытии вкладки экран
 * собирается уже после вызова [openSearch], и одноразовое событие до него бы не дошло.
 */
interface ICatalogSearchOpener {
    /** Есть необработанный запрос на фокус в поле поиска. */
    val isSearchFocusPending: StateFlow<Boolean>

    /** Переключает на вкладку каталога и запрашивает фокус в поле поиска. */
    fun openSearch()

    /** Экран каталога поставил фокус — запрос снят. */
    fun consumeSearchFocus()
}
