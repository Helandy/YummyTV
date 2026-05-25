package su.afk.yummy.tv.domain.account.model

enum class UserAnimeList(val id: Int) {
    WATCHING(0),
    PLANNED(1),
    COMPLETED(2),
    DROPPED(3),
    POSTPONED(5),
}
