package su.afk.yummy.tv.domain.home.model

enum class HomeFeedSectionType {
    SCHEDULE,
    /** Недавно вышедшие серии тайтлов из списков пользователя; собирается локально из расписания. */
    MY_NEW_EPISODES,
    NEW_RELEASES,
    RECOMMENDATIONS,
    COLLECTIONS,
}
