package su.afk.yummy.tv.core.model.settings

/**
 * Поле сортировки найденных раздач. У каждого поля свой «естественный» порядок (сиды и размер по убыванию,
 * название по алфавиту); [TorrentSettings.sortReversed] переворачивает его.
 */
enum class TorrentSort {
    SEEDERS,
    SIZE,
    TITLE,
}
