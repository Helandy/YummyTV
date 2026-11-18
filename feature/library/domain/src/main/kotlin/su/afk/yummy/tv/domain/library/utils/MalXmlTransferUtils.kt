package su.afk.yummy.tv.domain.library.utils

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import java.io.StringReader
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

private const val TAG_MYANIMELIST = "myanimelist"
private const val TAG_ANIME = "anime"
private const val TAG_MANGA = "manga"
private const val TAG_SERIES_ID = "series_animedb_id"
private const val TAG_SERIES_TITLE = "series_title"
private const val TAG_SERIES_TYPE = "series_type"
private const val TAG_SERIES_EPISODES = "series_episodes"
private const val TAG_WATCHED_EPISODES = "my_watched_episodes"
private const val TAG_TIMES_WATCHED = "my_times_watched"
private const val TAG_MY_SCORE = "my_score"
private const val TAG_MY_STATUS = "my_status"
private const val TAG_SHIKI_STATUS = "shiki_status"

/**
 * Потоково разбирает XML-экспорт списка аниме MyAnimeList или Shikimori с отключёнными
 * внешними сущностями (защита от XXE).
 */
fun parseMalXmlAnimeList(xml: String): ParsedExternalList {
    val handler = MalAnimeListSaxHandler()
    try {
        val factory = createSafeSaxParserFactory()
        val reader = factory.newSAXParser().xmlReader
        reader.contentHandler = handler
        reader.parse(InputSource(StringReader(xml)))
    } catch (error: SAXException) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    } catch (error: ExternalListParseException) {
        throw error
    } catch (error: Exception) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    }

    if (!handler.sawRootElement) {
        throw ExternalListParseException(ExternalListParseError.UNSUPPORTED_FORMAT)
    }

    val deduplicated = handler.entries.deduplicateExternalEntries()
    if (deduplicated.isEmpty()) {
        throw ExternalListParseException(ExternalListParseError.NO_VALID_ENTRIES)
    }

    return ParsedExternalList(
        format = ExternalListFormat.MAL_XML,
        entries = deduplicated,
        skippedNonAnimeCount = handler.skippedMangaCount,
        skippedInvalidCount = handler.skippedInvalidCount,
    )
}

/**
 * Формирует XML-файл в формате MyAnimeList, совместимый с импортом и на MyAnimeList, и на
 * Shikimori (`update_on_import = 1`, чтобы импортёр MAL применял все записи без ручной правки).
 */
fun formatMalXmlAnimeList(
    userName: String,
    entries: List<ExternalListEntry>,
): String {
    val watching = entries.count { it.list == UserAnimeList.WATCHING }
    val completed = entries.count { it.list == UserAnimeList.COMPLETED }
    val onHold = entries.count { it.list == UserAnimeList.POSTPONED }
    val dropped = entries.count { it.list == UserAnimeList.DROPPED }
    val planned = entries.count { it.list == UserAnimeList.PLANNED }
    val safeUserName = userName.ifBlank { "YummyTV" }.sanitizeXmlChars().escapeXmlText()

    return buildString(capacity = 512 + entries.size * 520) {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
        append("<!-- Exported by YummyTV -->\n")
        append("<myanimelist>\n")
        append("  <myinfo>\n")
        append("    <user_id>0</user_id>\n")
        append("    <user_name>").append(safeUserName).append("</user_name>\n")
        append("    <user_export_type>1</user_export_type>\n")
        append("    <user_total_anime>").append(entries.size).append("</user_total_anime>\n")
        append("    <user_total_watching>").append(watching).append("</user_total_watching>\n")
        append("    <user_total_completed>").append(completed).append("</user_total_completed>\n")
        append("    <user_total_onhold>").append(onHold).append("</user_total_onhold>\n")
        append("    <user_total_dropped>").append(dropped).append("</user_total_dropped>\n")
        append("    <user_total_plantowatch>").append(planned).append("</user_total_plantowatch>\n")
        append("  </myinfo>\n")

        entries.forEach { entry ->
            val seriesType = (entry.seriesType?.takeIf { it.isNotBlank() } ?: "Unknown")
                .sanitizeXmlChars()
                .escapeXmlText()
            append("  <anime>\n")
            append("    <series_animedb_id>").append(entry.externalId).append("</series_animedb_id>\n")
            append("    <series_title>").append(entry.title.toXmlCdata()).append("</series_title>\n")
            append("    <series_type>").append(seriesType).append("</series_type>\n")
            append("    <series_episodes>").append(entry.totalEpisodes.coerceAtLeast(0)).append("</series_episodes>\n")
            append("    <my_id>0</my_id>\n")
            append("    <my_watched_episodes>")
                .append(entry.watchedEpisodes.coerceAtLeast(0))
                .append("</my_watched_episodes>\n")
            append("    <my_start_date>0000-00-00</my_start_date>\n")
            append("    <my_finish_date>0000-00-00</my_finish_date>\n")
            append("    <my_rated></my_rated>\n")
            append("    <my_score>").append(entry.score.normalizedExternalScore() ?: 0).append("</my_score>\n")
            append("    <my_storage></my_storage>\n")
            append("    <my_storage_value>0.00</my_storage_value>\n")
            append("    <my_status>").append(entry.list.toMalStatus()).append("</my_status>\n")
            append("    <shiki_status>").append(entry.list.toShikimoriStatus()).append("</shiki_status>\n")
            append("    <my_comments><![CDATA[]]></my_comments>\n")
            append("    <my_times_watched>").append(entry.rewatches.coerceAtLeast(0)).append("</my_times_watched>\n")
            append("    <my_rewatch_value></my_rewatch_value>\n")
            append("    <my_priority>LOW</my_priority>\n")
            append("    <my_tags><![CDATA[]]></my_tags>\n")
            append("    <my_rewatching>0</my_rewatching>\n")
            append("    <my_rewatching_ep>0</my_rewatching_ep>\n")
            append("    <my_discuss>1</my_discuss>\n")
            append("    <my_sns>default</my_sns>\n")
            append("    <update_on_import>1</update_on_import>\n")
            append("  </anime>\n")
        }

        append("</myanimelist>\n")
    }
}

private fun createSafeSaxParserFactory(): SAXParserFactory =
    SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        isValidating = false
        setFeatureSafely(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeatureSafely("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeatureSafely("http://xml.org/sax/features/external-general-entities", false)
        setFeatureSafely("http://xml.org/sax/features/external-parameter-entities", false)
        setFeatureSafely("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }

private fun SAXParserFactory.setFeatureSafely(name: String, value: Boolean) {
    runCatching { setFeature(name, value) }
}

private fun String.sanitizeXmlChars(): String = buildString(length) {
    var i = 0
    while (i < this@sanitizeXmlChars.length) {
        val codePoint = this@sanitizeXmlChars.codePointAt(i)
        if (isValidXmlCodePoint(codePoint)) {
            appendCodePoint(codePoint)
        }
        i += Character.charCount(codePoint)
    }
}

private fun isValidXmlCodePoint(cp: Int): Boolean =
    cp == 0x9 ||
        cp == 0xA ||
        cp == 0xD ||
        cp in 0x20..0xD7FF ||
        cp in 0xE000..0xFFFD ||
        cp in 0x10000..0x10FFFF

private fun String.escapeXmlText(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun String.toXmlCdata(): String {
    val clean = sanitizeXmlChars()
    return "<![CDATA[" + clean.replace("]]>", "]]]]><![CDATA[>") + "]]>"
}

private class MalAnimeListSaxHandler : DefaultHandler() {
    val entries = mutableListOf<ExternalListEntry>()
    var sawRootElement: Boolean = false
    var skippedMangaCount: Int = 0
    var skippedInvalidCount: Int = 0

    private var depth: Int = 0
    private var insideAnime: Boolean = false
    private val textBuffer = StringBuilder()

    private var seriesId: String = ""
    private var seriesTitle: String = ""
    private var seriesType: String = ""
    private var seriesEpisodes: String = ""
    private var watchedEpisodes: String = ""
    private var timesWatched: String = ""
    private var myScore: String = ""
    private var myStatus: String = ""
    private var shikiStatus: String = ""

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
    ) {
        val tag = elementTag(localName, qName)
        depth++
        if (depth == 1) {
            if (tag == TAG_MYANIMELIST) {
                sawRootElement = true
            } else {
                throw ExternalListParseException(ExternalListParseError.UNSUPPORTED_FORMAT)
            }
        } else if (depth == 2 && tag == TAG_ANIME) {
            insideAnime = true
            resetCurrentAnime()
        } else if (depth == 2 && tag == TAG_MANGA) {
            skippedMangaCount++
        }
        if (insideAnime && depth == 3) {
            textBuffer.setLength(0)
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (insideAnime && depth == 3) {
            textBuffer.append(ch, start, length)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        val tag = elementTag(localName, qName)
        if (insideAnime && depth == 3) {
            val value = textBuffer.toString().trim()
            when (tag) {
                TAG_SERIES_ID -> seriesId = value
                TAG_SERIES_TITLE -> seriesTitle = value
                TAG_SERIES_TYPE -> seriesType = value
                TAG_SERIES_EPISODES -> seriesEpisodes = value
                TAG_WATCHED_EPISODES -> watchedEpisodes = value
                TAG_TIMES_WATCHED -> timesWatched = value
                TAG_MY_SCORE -> myScore = value
                TAG_MY_STATUS -> myStatus = value
                TAG_SHIKI_STATUS -> shikiStatus = value
            }
            textBuffer.setLength(0)
        } else if (insideAnime && depth == 2 && tag == TAG_ANIME) {
            insideAnime = false
            flushCurrentAnime()
        }
        depth--
    }

    private fun flushCurrentAnime() {
        val id = seriesId.toIntOrNull()?.takeIf { it > 0 }
        val status = myStatus.toUserAnimeListFromMal() ?: shikiStatus.toUserAnimeListFromShikimori()
        if (id == null || status == null) {
            skippedInvalidCount++
            return
        }
        entries += ExternalListEntry(
            externalId = id,
            title = seriesTitle,
            list = status,
            score = myScore.toIntOrNull().normalizedExternalScore(),
            watchedEpisodes = (watchedEpisodes.toIntOrNull() ?: 0).coerceAtLeast(0),
            rewatches = (timesWatched.toIntOrNull() ?: 0).coerceAtLeast(0),
            seriesType = seriesType.takeIf { it.isNotBlank() },
            totalEpisodes = (seriesEpisodes.toIntOrNull() ?: 0).coerceAtLeast(0),
        )
    }

    private fun resetCurrentAnime() {
        seriesId = ""
        seriesTitle = ""
        seriesType = ""
        seriesEpisodes = ""
        watchedEpisodes = ""
        timesWatched = ""
        myScore = ""
        myStatus = ""
        shikiStatus = ""
    }

    private fun elementTag(localName: String?, qName: String?): String =
        (qName?.takeIf { it.isNotBlank() } ?: localName.orEmpty()).lowercase(Locale.ROOT)
}
