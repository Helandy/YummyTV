package su.afk.yummy.tv.core.designsystem.presenter.mobile

data class MobileMainActions(
    val unreadNotificationsCount: Int,
    val avatarUrl: String,
    val onSettingsClick: () -> Unit,
    val onAccountClick: () -> Unit,
)
