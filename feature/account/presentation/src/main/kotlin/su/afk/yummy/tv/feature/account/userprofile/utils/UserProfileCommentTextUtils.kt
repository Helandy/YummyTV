package su.afk.yummy.tv.feature.account.userprofile.utils

private val spoilerRegex = Regex(
    pattern = "\\[спойлер(?:=\"[^\"]*\")?].*?\\[/спойлер]",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val leadingNickRegex = Regex("^\\s*\\[ник](.*?)\\[/ник]\\s*", RegexOption.IGNORE_CASE)
private val nickRegex = Regex("\\[ник](.*?)\\[/ник]", RegexOption.IGNORE_CASE)
private val bbCodeRegex = Regex("\\[/?[^\\]]+]")
private val whitespaceRegex = Regex("\\s+")

/** Ник из `[ник]…[/ник]` в начале комментария — кому он отвечает. */
internal fun String.commentReplyTo(): String? =
    leadingNickRegex.find(this)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Текст комментария для превью: ведущий `[ник]` убирается (он показывается отдельно),
 * остальные ники становятся «@ник», спойлеры прячутся за «…», BB-коды и лишние пробелы убираются.
 */
internal fun String.toCommentPreviewText(): String =
    replaceFirst(leadingNickRegex, "")
        .replace(nickRegex) { "@${it.groupValues[1]}" }
        .replace(spoilerRegex, "…")
        .replace(bbCodeRegex, "")
        .replace("&nbsp;", " ")
        .replace(whitespaceRegex, " ")
        .trim()
