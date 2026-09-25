package su.afk.yummy.tv.android.startup.model

/** Интерфейс, в который пришёл холодный старт. */
enum class StartupUi(val analyticsValue: String) {
    MOBILE("mobile"),
    TV("tv"),
}
