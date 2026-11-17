package su.afk.yummy.tv.feature.player.mobile.model

/** Страницы единого меню настроек мобильного плеера; [Root] — главный список. */
internal enum class MobilePlayerSettingsPage {
    Root,
    Quality,
    Speed,
    Subtitles,
    AudioTrack,
    SleepTimer,
    Resize,
    Display,

    /** Выбор режима улучшения видео; открывается из [Display] и возвращает туда же. */
    VideoEnhancement,
    CastAndPip,
}
