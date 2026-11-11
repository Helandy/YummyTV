package su.afk.yummy.tv.source.torrent.domain.model

import su.afk.yummy.tv.core.model.settings.TorrentSort

/**
 * Порядок раздач по настройке пользователя. Естественный порядок: сиды и размер по убыванию, название по
 * алфавиту без учёта регистра; [reversed] его переворачивает. При равенстве ключа сначала больше сидов,
 * чтобы выдача не «прыгала» между равными по размеру раздачами.
 */
fun List<TorrentResult>.sortedFor(sort: TorrentSort, reversed: Boolean): List<TorrentResult> {
    val natural: Comparator<TorrentResult> = when (sort) {
        TorrentSort.SEEDERS -> compareByDescending<TorrentResult> { it.seeders }
        TorrentSort.SIZE -> compareByDescending<TorrentResult> { it.sizeBytes }
        TorrentSort.TITLE -> compareBy<TorrentResult, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    }
    val primary = if (reversed) natural.reversed() else natural
    return sortedWith(primary.thenByDescending { it.seeders })
}
