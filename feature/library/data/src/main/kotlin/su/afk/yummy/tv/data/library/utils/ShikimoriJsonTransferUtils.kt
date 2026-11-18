package su.afk.yummy.tv.data.library.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.data.library.dto.ShikimoriListEntryDto
import su.afk.yummy.tv.data.library.mapper.isNonAnimeTarget
import su.afk.yummy.tv.data.library.mapper.toExternalListEntry
import su.afk.yummy.tv.data.library.mapper.toShikimoriDto
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.utils.MAX_IMPORT_DECOMPRESSED_BYTES
import su.afk.yummy.tv.domain.library.utils.decodeExternalListText
import su.afk.yummy.tv.domain.library.utils.deduplicateExternalEntries
import su.afk.yummy.tv.domain.library.utils.detectExternalListFormat
import su.afk.yummy.tv.domain.library.utils.parseMalXmlAnimeList

@OptIn(ExperimentalSerializationApi::class)
private val ShikimoriImportJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

@OptIn(ExperimentalSerializationApi::class)
private val ShikimoriExportJson = Json {
    prettyPrint = true
    encodeDefaults = false
    explicitNulls = true
}

private val ShikimoriListSerializer = ListSerializer(ShikimoriListEntryDto.serializer())

/**
 * Автоматически определяет формат входных байтов (MAL XML, MAL `.xml.gz` или Shikimori JSON) и
 * разбирает список аниме.
 */
internal fun parseExternalListBytes(
    bytes: ByteArray,
    maxDecompressedBytes: Int = MAX_IMPORT_DECOMPRESSED_BYTES,
): ParsedExternalList {
    val text = bytes.decodeExternalListText(maxDecompressedBytes)
    return when (detectExternalListFormat(text)) {
        ExternalListFormat.MAL_XML -> parseMalXmlAnimeList(text)
        ExternalListFormat.SHIKIMORI_JSON -> parseShikimoriJsonAnimeList(text)
    }
}

/** Разбирает JSON-экспорт списка аниме Shikimori (`*_animes.json`). */
internal fun parseShikimoriJsonAnimeList(json: String): ParsedExternalList {
    val rawItems = try {
        ShikimoriImportJson.decodeFromString(ShikimoriListSerializer, json)
    } catch (error: SerializationException) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    } catch (error: IllegalArgumentException) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    }

    var skippedNonAnime = 0
    var skippedInvalid = 0
    val parsed = ArrayList<ExternalListEntry>(rawItems.size)

    rawItems.forEach { dto ->
        if (dto.isNonAnimeTarget()) {
            skippedNonAnime++
            return@forEach
        }
        val entry = dto.toExternalListEntry()
        if (entry == null) {
            skippedInvalid++
        } else {
            parsed += entry
        }
    }

    val deduplicated = parsed.deduplicateExternalEntries()
    if (deduplicated.isEmpty()) {
        throw ExternalListParseException(ExternalListParseError.NO_VALID_ENTRIES)
    }

    return ParsedExternalList(
        format = ExternalListFormat.SHIKIMORI_JSON,
        entries = deduplicated,
        skippedNonAnimeCount = skippedNonAnime,
        skippedInvalidCount = skippedInvalid,
    )
}

/** Сериализует список в канонический JSON-формат экспорта Shikimori. */
internal fun formatShikimoriJsonAnimeList(entries: List<ExternalListEntry>): String =
    ShikimoriExportJson.encodeToString(
        ShikimoriListSerializer,
        entries.map(ExternalListEntry::toShikimoriDto),
    ) + "\n"
