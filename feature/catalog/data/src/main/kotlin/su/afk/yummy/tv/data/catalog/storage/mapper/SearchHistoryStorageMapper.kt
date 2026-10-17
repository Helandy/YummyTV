package su.afk.yummy.tv.data.catalog.storage.mapper

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val searchHistorySerializer = ListSerializer(String.serializer())

/** Нечитаемая запись считается пустой историей: терять из-за неё поиск незачем. */
internal fun String?.toSearchHistory(): List<String> =
    this?.let { raw -> runCatching { Json.decodeFromString(searchHistorySerializer, raw) }.getOrNull() }
        .orEmpty()

internal fun List<String>.toSearchHistoryStorage(): String =
    Json.encodeToString(searchHistorySerializer, this)
