package su.afk.yummy.tv.core.model.settings

/**
 * Какие списки пользователя попадают в блок новых серий на главной. Отдельный enum, а не
 * `UserAnimeList` из account-домена: настройки живут в core и о фичах не знают.
 */
enum class NewEpisodesSource {
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,
    FAVORITES,
    ;

    companion object {
        /** По умолчанию блок следит за «Смотрю» и избранным. */
        val DEFAULT: Set<NewEpisodesSource> = setOf(WATCHING, FAVORITES)
    }
}
