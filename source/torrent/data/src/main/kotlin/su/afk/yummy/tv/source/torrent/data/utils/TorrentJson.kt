package su.afk.yummy.tv.source.torrent.data.utils

import kotlinx.serialization.json.Json

/** Свой разбор ответов: индексаторы и TorrServer отдают `text/json` и лишние поля, общий ContentNegotiation их не примет. */
internal val torrentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}
