package su.afk.yummy.tv.core.navigation.root

enum class RootTab {
    ACCOUNT,
    CATALOG,
    HOME,
    POSTS,

    /** Хаб «Сообщество» вместо новостей в нижней панели; есть только на мобилке. */
    COMMUNITY,
    COLLECTIONS,
    SCHEDULE,
    LIBRARY,

    /** История просмотров отдельным пунктом меню; есть только на ТВ. */
    HISTORY,
    SETTINGS,
}
