package su.afk.yummy.tv.core.model.settings

/** Тип индексатора раздач, по которому ищем торренты. */
enum class TorrentIndexer {
    /** Публичный JacRed API (`/api/v1.0/torrents`), ключ не нужен. */
    JACRED,

    /** Jackett: `/api/v2.0/indexers/all/results`, нужен API-ключ. */
    JACKETT,
}
