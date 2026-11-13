package su.afk.yummy.tv.feature.home.model

/** Итог проверки серверного объявления. */
internal sealed interface HomeAnnouncementResult {
    /** Объявление выключено или не заполнено. */
    data object Disabled : HomeAnnouncementResult

    /** Объявление включено, но пользователь его уже прочитал. */
    data object AlreadySeen : HomeAnnouncementResult

    /** Объявление нужно показать. */
    data class Show(val announcement: HomeAnnouncement) : HomeAnnouncementResult
}
