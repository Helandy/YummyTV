package su.afk.yummy.tv.domain.home.model

enum class HomeFeedSectionType {
    SCHEDULE,
    /** Недавно вышедшие серии тайтлов из списков пользователя; собирается локально из расписания. */
    MY_NEW_EPISODES,
    /** Свежие серии (`/feed` new_videos). */
    NEW_RELEASES,
    /** Новые тайтлы (`/feed` new). */
    NEW_TITLES,
    RECOMMENDATIONS,
    /** Анонсы, которые скоро выйдут (`/feed` announcements). */
    ANNOUNCEMENTS,
    COLLECTIONS,
}
