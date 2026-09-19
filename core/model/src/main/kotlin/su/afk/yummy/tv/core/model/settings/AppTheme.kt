package su.afk.yummy.tv.core.model.settings

enum class AppTheme {
    WARM_AMBER,
    SAKURA,
    MINT,
    OCEAN,
    GRAPHITE,

    /**
     * Системная палитра Material You: цвета берутся из обоев/акцента системы (Android 12+).
     * Доступна только на мобильном интерфейсе; на более старых версиях тема откатывается
     * на [WARM_AMBER] (см. YummyTvTheme).
     */
    DYNAMIC,
}
