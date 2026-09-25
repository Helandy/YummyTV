package su.afk.yummy.tv.android.startup.model

/** Чем запущен процесс: иконкой лаунчера, диплинком или другим Intent (поиск, уведомление). */
enum class StartupEntry(val analyticsValue: String) {
    LAUNCHER("launcher"),
    DEEPLINK("deeplink"),
    OTHER("other"),
}
