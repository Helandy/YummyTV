package su.afk.yummy.tv.source.torrent.data.utils

import su.afk.yummy.tv.core.model.settings.TorrentSettings

/**
 * Состояние настроек торрентов для лога: без адресов, логина, пароля и ключа Jackett — только признаки того,
 * что задано, чтобы по логу пользователя понять, почему источник не показывается.
 */
internal fun TorrentSettings.logSummary(): String =
    "enabled=$enabled torrServerSet=${torrServerUrl.isNotBlank()} jacRed=$jacRedEnabled " +
        "jackett=$jackettEnabled jackettUrlSet=${jackettUrl.isNotBlank()} " +
        "indexers=${activeIndexers.map { it.name }} configured=$isConfigured"

/**
 * Класс ошибки (и причины) для лога. Текст сообщения не пишем: в сообщениях Ktor есть полный URL запроса, а в нём
 * API-ключ Jackett. Статус ответа индексатора — наше сообщение `indexer HTTP <код>`, оно безопасно.
 */
internal fun Throwable.logSummary(): String {
    val status = message?.takeIf { it.startsWith(INDEXER_HTTP_PREFIX) }
    val cause = cause?.let { " cause=${it::class.java.simpleName}" }.orEmpty()
    return "${this::class.java.simpleName}${status?.let { " ($it)" }.orEmpty()}$cause"
}

internal const val INDEXER_HTTP_PREFIX = "indexer HTTP"
