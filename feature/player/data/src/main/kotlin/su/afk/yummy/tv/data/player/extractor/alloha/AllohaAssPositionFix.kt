package su.afk.yummy.tv.data.player.extractor.alloha

/**
 * Media3's ASS/SSA decoder never reads Style/Dialogue `MarginV`/`MarginL`/`MarginR` (see the
 * `// TODO: Read the MarginL, MarginR and MarginV values...` in androidx.media3's SsaParser) - it
 * always falls back to its own fixed default margin, which shows up as subtitles glued to the
 * bottom edge regardless of what the track author specified. Media3 DOES honor an explicit
 * `\pos(x,y)` override tag though (as long as `PlayResX`/`PlayResY` are set), so this rewrites each
 * Dialogue line's resolved alignment + margins into an equivalent `\pos` tag - the same placement
 * libass itself uses - so playback matches a full ASS renderer (e.g. Alloha's own website player).
 */

private const val SYNTHETIC_PLAY_RES = 1000

private val SECTION_HEADER = Regex("""^\s*\[(.+)]\s*$""")
private val POSITIONED_ALREADY = Regex("""\\(pos|move)\s*\(""")

private class AssFieldFormat(fields: List<String>) {
    private val fields = fields.map(String::trim)
    val fieldCount get() = fields.size
    fun indexOf(name: String) = fields.indexOfFirst { it.equals(name, ignoreCase = true) }
}

private data class AssStyle(
    val alignment: Int,
    val marginL: Int,
    val marginR: Int,
    val marginV: Int
)

private class AssSection(val name: String, val range: IntRange)

fun fixAssMarginPositions(assText: String): String {
    val lines = assText.replace("\r\n", "\n").split("\n")
    val sections = locateSections(lines)

    fun section(predicate: (String) -> Boolean) = sections.firstOrNull { predicate(it.name) }
    val scriptInfo = section { it == "script info" }
    val styles = section { it.startsWith("v4") && it.contains("styles") }
    val events = section { it == "events" }

    val (playResX, playResY) = readPlayRes(lines, scriptInfo)
    val resX = playResX ?: SYNTHETIC_PLAY_RES
    val resY = playResY ?: SYNTHETIC_PLAY_RES

    val styleMap = parseStyles(lines, styles)
    val output = lines.toMutableList()
    rewriteDialoguePositions(lines, events, styleMap, resX, resY, output)

    // Вставляем только отсутствующий ключ: второй уже есть в файле, дубль перекрыл бы настоящее
    // значение выдуманным.
    if (playResX == null || playResY == null) {
        insertSyntheticPlayRes(
            output = output,
            scriptInfo = scriptInfo,
            missingX = playResX == null,
            missingY = playResY == null,
            resX = resX,
            resY = resY,
        )
    }

    return output.joinToString("\n")
}

private fun locateSections(lines: List<String>): List<AssSection> = buildList {
    var currentStart = -1
    var currentName: String? = null
    lines.forEachIndexed { index, raw ->
        val match = SECTION_HEADER.matchEntire(raw) ?: return@forEachIndexed
        currentName?.let { add(AssSection(it, currentStart until index)) }
        currentName = match.groupValues[1].trim().lowercase()
        currentStart = index + 1
    }
    currentName?.let { add(AssSection(it, currentStart until lines.size)) }
}

private fun readPlayRes(lines: List<String>, scriptInfo: AssSection?): Pair<Int?, Int?> {
    var playResX: Int? = null
    var playResY: Int? = null
    scriptInfo?.range?.forEach { i ->
        val (key, value) = lines[i].splitKeyValue() ?: return@forEach
        when (key.lowercase()) {
            "playresx" -> playResX = value.toIntOrNull()
            "playresy" -> playResY = value.toIntOrNull()
        }
    }
    return playResX to playResY
}

private fun parseStyles(lines: List<String>, styles: AssSection?): Map<String, AssStyle> {
    val styleMap = mutableMapOf<String, AssStyle>()
    var format: AssFieldFormat? = null
    styles?.range?.forEach { i ->
        val raw = lines[i].trim()
        when {
            raw.startsWith("Format:", ignoreCase = true) ->
                format = AssFieldFormat(raw.removePrefix("Format:").split(','))

            raw.startsWith("Style:", ignoreCase = true) -> {
                val currentFormat = format ?: return@forEach
                val values = raw.removePrefix("Style:").split(',').map(String::trim)
                val nameIdx = currentFormat.indexOf("Name")
                val alignIdx = currentFormat.indexOf("Alignment")
                if (nameIdx < 0 || alignIdx < 0) return@forEach
                val name = values.getOrNull(nameIdx) ?: return@forEach
                styleMap[name] = AssStyle(
                    alignment = values.getOrNull(alignIdx)?.toIntOrNull() ?: 2,
                    marginL = values.getOrNull(currentFormat.indexOf("MarginL"))?.toIntOrNull()
                        ?: 0,
                    marginR = values.getOrNull(currentFormat.indexOf("MarginR"))?.toIntOrNull()
                        ?: 0,
                    marginV = values.getOrNull(currentFormat.indexOf("MarginV"))?.toIntOrNull()
                        ?: 0,
                )
            }
        }
    }
    return styleMap
}

private fun rewriteDialoguePositions(
    lines: List<String>,
    events: AssSection?,
    styleMap: Map<String, AssStyle>,
    resX: Int,
    resY: Int,
    output: MutableList<String>,
) {
    var format: AssFieldFormat? = null
    events?.range?.forEach { i ->
        val raw = lines[i]
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("Format:", ignoreCase = true) ->
                format = AssFieldFormat(trimmed.removePrefix("Format:").split(','))

            trimmed.startsWith("Dialogue:", ignoreCase = true) -> {
                val currentFormat = format ?: return@forEach
                val body = raw.substringAfter(':', "")
                val fields = body.split(',', limit = currentFormat.fieldCount)
                if (fields.size < currentFormat.fieldCount) return@forEach
                val textIdx = currentFormat.indexOf("Text")
                if (textIdx < 0) return@forEach
                val text = fields[textIdx]
                if (POSITIONED_ALREADY.containsMatchIn(text)) return@forEach

                val style = currentFormat.indexOf("Style").takeIf { it >= 0 }
                    ?.let { fields.getOrNull(it)?.trim() }
                    ?.let(styleMap::get)
                val lineMargin = { name: String ->
                    currentFormat.indexOf(name).takeIf { it >= 0 }
                        ?.let { fields.getOrNull(it)?.trim()?.toIntOrNull() }
                        ?.takeIf { it != 0 }
                }

                val alignment = style?.alignment ?: 2
                val marginL = lineMargin("MarginL") ?: style?.marginL ?: 0
                val marginR = lineMargin("MarginR") ?: style?.marginR ?: 0
                val marginV = lineMargin("MarginV") ?: style?.marginV ?: 0

                val y = when (alignment) {
                    in 7..9 -> marginV
                    in 4..6 -> resY / 2
                    else -> resY - marginV
                }
                val x = when (alignment % 3) {
                    1 -> marginL
                    2 -> resX / 2
                    else -> resX - marginR
                }

                val newFields = fields.toMutableList()
                newFields[textIdx] = "{\\pos($x,$y)}$text"
                output[i] = "Dialogue:" + newFields.joinToString(",")
            }
        }
    }
}

private fun insertSyntheticPlayRes(
    output: MutableList<String>,
    scriptInfo: AssSection?,
    missingX: Boolean,
    missingY: Boolean,
    resX: Int,
    resY: Int,
) {
    val insertionIndex = scriptInfo?.range?.first ?: run {
        output.add(0, "[Script Info]")
        1
    }
    if (missingX) output.add(insertionIndex, "PlayResX: $resX")
    if (missingY) output.add(insertionIndex, "PlayResY: $resY")
}

private fun String.splitKeyValue(): Pair<String, String>? {
    val idx = indexOf(':')
    if (idx < 0) return null
    return substring(0, idx).trim() to substring(idx + 1).trim()
}
