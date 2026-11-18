package su.afk.yummy.tv.domain.library.model

/** Причина отказа при разборе входного файла списка аниме. */
enum class ExternalListParseError {
    EMPTY_FILE,
    FILE_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    MALFORMED_CONTENT,
    NO_VALID_ENTRIES,
}
