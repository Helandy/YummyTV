package su.afk.yummy.tv.core.common.player

/** Текущий шаг получения потока: показывается пользователю, чтобы было видно, где именно возник сбой. */
enum class PlayerStreamResolveStatus {
    Preparing,
    FetchingPage,

    /** Источник ответил успешно, дальше идёт разбор ответа. */
    ParsingPage,

    /** Источник ответил ошибкой (не 2xx). */
    ResponseError,

    /** Загрузка скрипта плеера, из которого берётся адрес эндпоинта. */
    LoadingPlayerScript,

    /** Выбор озвучки и серии в списке источника. */
    SelectingDubbing,
    RequestingStream,

    /** Расшифровка ссылок из ответа источника. */
    DecodingLinks,
    CheckingQualities,
    OpeningSession,
    Retrying,
    WaitingForAd,

    /** Поиск нужной серии или озвучки в списке источника. */
    SearchingEpisode,

    /** Извлечение ссылки на видео из страницы или плеера. */
    ExtractingLink,

    /** Загрузка плейлиста со списком качеств. */
    LoadingPlaylist,

    /** Anime4k: вопрос о рекламе. */
    AskingConsent,

    /** Anime4k: получение тикета на просмотр. */
    RequestingTicket,

    /** Торренты: на экране выбора раздачи, ждём выбор пользователя. */
    WaitingForTorrent,

    /** Торренты: на экране выбора файла раздачи, ждём выбор пользователя. */
    WaitingForTorrentFile,

    /** Итог: нужной серии нет на сайте источника. */
    EpisodeNotFound,

    /** Итог: на странице нет плеера для серии. */
    PlayerNotFound,

    /** Итог: плеер найден, но ссылки на видео в нём нет. */
    LinkNotFound,
}
