package su.afk.yummy.tv.core.analytics.api

/**
 * Теги [AnalyticsTracker.log], которые в release сохраняются в файл логов приложения
 * (Настройки → О приложении → Поделиться логами), если запись логов включена в Настройки → Общие.
 * Остальные теги в release по-прежнему no-op.
 *
 * Это единственное место, где решается, что именно сохраняется: модули плеера берут теги отсюда,
 * поэтому переименование не отключит запись молча.
 */
object PersistedLogTags {
    const val PLAYER_BUFFERING = "PlayerBuffering"
    const val PLAYER_VIEW_MODEL = "PlayerViewModel"
    const val PLAYER_MEDIA_SESSION = "PlayerMediaSession"
    const val PLAYER_LOUDNESS = "PlayerLoudness"
    const val PLAYER_EXTRACTOR = "PlayerExtractor"
    const val ALLOHA_EXTRACTOR = "AllohaExtractor"
    const val ALLOHA_STREAM_PROXY = "AllohaStreamProxy"
    const val CVH_EXTRACTOR = "CvhExtractor"

    val persisted: Set<String> = setOf(
        PLAYER_BUFFERING,
        PLAYER_VIEW_MODEL,
        PLAYER_MEDIA_SESSION,
        PLAYER_LOUDNESS,
        PLAYER_EXTRACTOR,
        ALLOHA_EXTRACTOR,
        ALLOHA_STREAM_PROXY,
        CVH_EXTRACTOR,
    )
}
