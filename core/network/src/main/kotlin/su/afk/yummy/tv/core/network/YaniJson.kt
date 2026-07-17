package su.afk.yummy.tv.core.network

import kotlinx.serialization.json.Json

/** Единая конфигурация Json для всех запросов к API yani.tv. */
val YaniApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
