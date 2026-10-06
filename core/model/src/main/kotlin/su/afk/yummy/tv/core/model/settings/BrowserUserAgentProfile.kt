package su.afk.yummy.tv.core.model.settings

/**
 * User-Agent, под которым приложение ходит к балансерам и их CDN (кроме Alloha, у которой свой
 * отпечаток сессии).
 *
 * Это закрытый список, а не свободный текст: CDN принимает не любой «браузерный» UA. okcdn (CVH)
 * на живой ссылке отвечает `400` на Chrome под macOS, Safari, Firefox, `okhttp`, пустой UA и
 * Chrome под Android без `Mobile`; все профили ниже проверены тем же замером и получают `206`.
 * Добавлять сюда строку можно только после такой проверки (методика — `docs/cvh-player.md`).
 */
enum class BrowserUserAgentProfile(val userAgent: String) {
    /** По умолчанию. */
    WINDOWS_CHROME(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/149.0.0.0 Safari/537.36",
    ),
    LINUX_CHROME(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/149.0.0.0 Safari/537.36",
    ),
    ANDROID_CHROME(
        "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/125 Mobile Safari/537.36",
    ),
    ;

    companion object {
        val DEFAULT = WINDOWS_CHROME
    }
}
