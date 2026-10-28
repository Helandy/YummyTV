package su.afk.yummy.tv.core.model.settings

data class MainSettingsSnapshot(
    val appTheme: AppTheme,
    val backgroundStyle: BackgroundStyle,
    val navTransitionStyle: NavTransitionStyle = NavTransitionStyle.FADE_SCALE,
    val posterQuality: PosterQuality,
    val posterCardSize: PosterCardSize,
    val yaniNickname: String,
    val yaniAvatarUrl: String,
    val yaniUnreadNotificationsCount: Int,
    val seasonalDecorationsEnabled: Boolean = true,
    /** Ключ праздничного периода, о котором пользователю уже сообщили (см. `seasonalAnnouncementKey`). */
    val seasonalAnnouncedKey: String = "",
)
