package su.afk.yummy.tv.domain.home.model

/**
 * Итог одного прогона выбора локальных пушей.
 *
 * @property pushes серии, о которых нужно уведомить сейчас.
 * @property knownKeys ключи серий, которые прогон учёл (в том числе просмотренные, устаревшие и
 * подавленные дедупом), плюс отметка первой базовой линии — их нужно запомнить, чтобы не вернуться
 * к ним в следующий раз.
 */
data class LibraryEpisodePushSelection(
    val pushes: List<LibraryEpisodePush>,
    val knownKeys: Set<String>,
)
