package su.afk.yummy.tv.domain.library.model

/** Этап переноса списка, на котором произошёл сетевой или серверный сбой. */
enum class AnimeListTransferStage {
    /** Пакетное сопоставление внешних ID Shikimori/MAL с каталогом YummyAnime при импорте. */
    RESOLVE_EXTERNAL_IDS,

    /** Загрузка текущего списка библиотеки авторизованного пользователя (`GET /users/{id}/lists`). */
    LOAD_USER_LIBRARY,

    /** Пакетная подгрузка внешних ID для локальной библиотеки гостя (`GET /anime?ids=...`). */
    ENRICH_GUEST_LIBRARY,
}
