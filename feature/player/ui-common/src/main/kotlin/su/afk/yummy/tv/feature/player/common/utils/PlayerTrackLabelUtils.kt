package su.afk.yummy.tv.feature.player.common.utils

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import java.util.Locale

/**
 * Подпись дорожки для меню: «Русский · Studio Band · AC3 5.1».
 *
 * В MKV названия дорожек бывают пустыми, «Track 2» или просто кодом языка (`rus`), поэтому собираем
 * подпись из языка, названия и (для звука) кодека с раскладкой каналов. Если ничего нет — [fallback].
 */
internal fun Format.readableTrackLabel(
    fallback: String,
    locale: Locale = Locale.getDefault(),
): String = readableTrackLabel(
    language = language,
    label = label,
    sampleMimeType = sampleMimeType,
    channelCount = channelCount,
    fallback = fallback,
    locale = locale,
)

internal fun readableTrackLabel(
    language: String?,
    label: String?,
    sampleMimeType: String?,
    channelCount: Int,
    fallback: String,
    locale: Locale = Locale.getDefault(),
): String {
    val languageName = language?.toLanguageName(locale)
    val title = label?.trim()?.takeIf { raw ->
        raw.isNotEmpty() &&
            !raw.equals(language, ignoreCase = true) &&
            !raw.equals(languageName, ignoreCase = true)
    }
    val head = when {
        title != null && languageName != null && !title.contains(languageName, ignoreCase = true) ->
            "$languageName · $title"

        title != null -> title
        languageName != null -> languageName
        else -> fallback
    }
    val technical = audioTechnicalInfo(sampleMimeType, channelCount)
    return if (technical.isEmpty()) head else "$head · $technical"
}

/** Делает подписи в списке уникальными: одинаковые получают номер, чтобы дорожки не путались. */
internal fun List<String>.disambiguated(): List<String> {
    val total = groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return map { label ->
        if (total.getValue(label) == 1) {
            label
        } else {
            val number = (seen[label] ?: 0) + 1
            seen[label] = number
            "$label ($number)"
        }
    }
}

private fun audioTechnicalInfo(sampleMimeType: String?, channelCount: Int): String {
    if (sampleMimeType == null || !MimeTypes.isAudio(sampleMimeType)) return ""
    return listOfNotNull(audioCodecName(sampleMimeType), channelLayoutName(channelCount)).joinToString(" ")
}

private fun audioCodecName(sampleMimeType: String): String? = when (sampleMimeType) {
    MimeTypes.AUDIO_AC3 -> "AC3"
    MimeTypes.AUDIO_E_AC3 -> "E-AC3"
    MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3 Atmos"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_DTS -> "DTS"
    MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
    MimeTypes.AUDIO_DTS_EXPRESS -> "DTS Express"
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    else -> null
}

private fun channelLayoutName(channelCount: Int): String? = when (channelCount) {
    Format.NO_VALUE, 0 -> null
    1 -> "1.0"
    2 -> "2.0"
    3 -> "2.1"
    6 -> "5.1"
    8 -> "7.1"
    else -> "${channelCount}ch"
}

private fun String.toLanguageName(locale: Locale): String? {
    if (isBlank() || equals(C.LANGUAGE_UNDETERMINED, ignoreCase = true)) return null
    val name = Locale.forLanguageTag(this).getDisplayLanguage(locale)
    // Неизвестный код Locale возвращает как есть — такую подпись не показываем.
    if (name.isBlank() || name.equals(this, ignoreCase = true)) return null
    return name.replaceFirstChar { it.titlecase(locale) }
}
