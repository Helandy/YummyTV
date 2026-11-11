package su.afk.yummy.tv.core.model.settings

/** Снимок настроек торрент-источника. */
data class TorrentSettings(
    val enabled: Boolean = false,
    val torrServerUrl: String = "",
    val torrServerUser: String = "",
    val torrServerPassword: String = "",
    /** Искать через JacRed. Можно вместе с Jackett: результаты объединяются. */
    val jacRedEnabled: Boolean = true,
    /** Искать через свой Jackett. */
    val jackettEnabled: Boolean = false,
    /** Адрес JacRed; пустой — используется [DEFAULT_INDEXER_URL]. */
    val jacRedUrl: String = DEFAULT_INDEXER_URL,
    /** Адрес своего Jackett: у него нет адреса по умолчанию. */
    val jackettUrl: String = "",
    val jackettApiKey: String = "",
    val sort: TorrentSort = TorrentSort.SEEDERS,
    /** Перевернуть естественный порядок [sort]. */
    val sortReversed: Boolean = false,
) {
    /** Адрес JacRed с учётом значения по умолчанию. */
    val effectiveJacRedUrl: String
        get() = jacRedUrl.ifBlank { DEFAULT_INDEXER_URL }

    /** Индексаторы, по которым реально можно искать: включены и (для Jackett) указан адрес. */
    val activeIndexers: List<TorrentIndexer>
        get() = buildList {
            if (jacRedEnabled) add(TorrentIndexer.JACRED)
            if (jackettEnabled && jackettUrl.isNotBlank()) add(TorrentIndexer.JACKETT)
        }

    /** Источник готов к работе: включён, указан адрес TorrServer и есть хотя бы один рабочий индексатор. */
    val isConfigured: Boolean
        get() = enabled && torrServerUrl.isNotBlank() && activeIndexers.isNotEmpty()

    companion object {
        const val DEFAULT_INDEXER_URL = "https://jac-red.ru"

        /** Подсказка в поле адреса Jackett (стандартный порт Jackett — 9117). */
        const val JACKETT_URL_HINT = "http://192.168.1.10:9117"
    }
}
