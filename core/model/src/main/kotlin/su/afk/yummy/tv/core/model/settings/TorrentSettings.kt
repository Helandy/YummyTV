package su.afk.yummy.tv.core.model.settings

/** Снимок настроек торрент-источника. */
data class TorrentSettings(
    val enabled: Boolean = false,
    val torrServerUrl: String = "",
    val torrServerUser: String = "",
    val torrServerPassword: String = "",
    val indexer: TorrentIndexer = TorrentIndexer.JACRED,
    val indexerUrl: String = DEFAULT_INDEXER_URL,
    val jackettApiKey: String = "",
) {
    /** Источник готов к работе: включён и указан адрес TorrServer. */
    val isConfigured: Boolean
        get() = enabled && torrServerUrl.isNotBlank() && indexerUrl.isNotBlank()

    companion object {
        const val DEFAULT_INDEXER_URL = "https://jac-red.ru"
    }
}
