package su.afk.yummy.tv.domain.anime.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips

private val TYPE_PREFIXES = listOf("озвучка", "субтитры", "дубляж", "dub", "sub")
private val LANGUAGE_TAG = Regex("""^\s*[(\[][^)\]]{1,12}[)\]]\s*""")
private val QUALITY_TAG = Regex("""(?:\s+в)?\s*\[?(?<![\p{L}\d])4[kк](?![\p{L}\d])\]?""", RegexOption.IGNORE_CASE)
private val SUBTITLES_SUFFIX = Regex("""[.\s]*subtitles?$""", RegexOption.IGNORE_CASE)
private val DOMAIN_SUFFIX = Regex("""\.(tv|ru|org|com|net|me|su|io|cc)\b""", RegexOption.IGNORE_CASE)
private val SEPARATORS = Regex("""\s*[&+/,]\s*""")
private val WORD_SPLIT = Regex("""[\s\-_]+""")
private val ADJECTIVE_ENDING = Regex("""(ый|ий|ой|ая|ое|ые)$""")
private val NOISE_WORDS = setOf("project", "studio", "team", "online", "dub", "dubbing")
private val PIPE_TYPES = setOf("субтитры", "озвучка")
private val GLUED_NOISE_SUFFIXES = listOf("studio", "project", "team", "online")

/** Ключи после снятия типа и домена; значения — точные ключи, у которых нет общего написания. */
private val ALIASES = mapOf(
    "анилибрия" to "anilibria",
    "анидаб" to "anidub",
    "анидуб" to "anidub",
    "ort" to "орт",
    "shizaproject" to "shiza",
    "дубляжная" to "тодубляжная",
    "amedia" to "animedia",
    "амедиа" to "animedia",
    "анимедиа" to "animedia",
    "анистар" to "anistar",
)

/** Тип озвучки: озвучка и субтитры одной студии — разные дорожки, их сливать нельзя. */
private enum class DubbingKind { VOICE, SUBTITLES }

/** [explicitKind] — тип назван в самом имени («Субтитры …», «….Subtitles», «…|Субтитры»), а не угадан. */
private class ParsedDubbing(val kind: DubbingKind, val text: String, val explicitKind: Boolean)

private fun String.stripTypePrefixOnce(): String {
    val lower = lowercase()
    val prefix = TYPE_PREFIXES.firstOrNull { lower.startsWith("$it ") } ?: return this
    return substring(prefix.length).trim()
}

private fun String.stripTypePrefixes(): String {
    var current = this
    while (true) {
        val next = current.stripTypePrefixOnce()
        if (next == current) return current
        current = next
    }
}

/** Тип и название без одного типа-префикса, языкового тега, `[4K]`, суффикса `.Subtitles` и `|Тип`. */
private fun String.parseDubbing(): ParsedDubbing {
    var text = replace("\uFEFF", "").replace(LANGUAGE_TAG, "").replace(QUALITY_TAG, "").trim()
    var kind = DubbingKind.VOICE
    var explicit = false
    val pipeType = text.substringAfterLast('|', "").trim().lowercase()
    if (text.contains('|') && pipeType in PIPE_TYPES) {
        kind = if (pipeType == "субтитры") DubbingKind.SUBTITLES else DubbingKind.VOICE
        explicit = true
        text = text.substringBeforeLast('|').trim()
    } else if (SUBTITLES_SUFFIX.containsMatchIn(text) && !text.equals("subtitles", ignoreCase = true)) {
        kind = DubbingKind.SUBTITLES
        explicit = true
        text = text.replace(SUBTITLES_SUFFIX, "").trim()
    } else {
        val lower = text.lowercase()
        if (lower.startsWith("субтитры") || lower == "sub" || lower.startsWith("sub ")) {
            kind = DubbingKind.SUBTITLES
            explicit = true
        } else if (TYPE_PREFIXES.any { lower.startsWith("$it ") }) {
            explicit = true
        }
    }
    return ParsedDubbing(kind, text.stripTypePrefixOnce(), explicit)
}

private fun String.compact(): String =
    lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }

private fun String.withoutDomain(): String = replace(DOMAIN_SUFFIX, "")

private fun String.aliased(): String = ALIASES[this] ?: this

/** Точный ключ: регистр, домен, пробелы, дефисы и скобки не важны; тип-префикс снят один раз. */
private fun ParsedDubbing.exactKey(): String = text.withoutDomain().compact().aliased()

/** Ключ без вложенных типов-префиксов («Дубляж Мосфильм-Мастер» → «Мосфильм-Мастер»). */
private fun ParsedDubbing.baseKey(): String =
    text.stripTypePrefixes().withoutDomain().compact().aliased()

/** Мягкий ключ: без служебных слов (Project/Studio/Team/Online) и родовых окончаний. */
private fun ParsedDubbing.looseKey(): String =
    text.stripTypePrefixes().withoutDomain()
        .split(WORD_SPLIT)
        .filter { it.lowercase() !in NOISE_WORDS }
        .joinToString("")
        .compact()
        .aliased()
        .let { key ->
            val suffix = GLUED_NOISE_SUFFIXES.firstOrNull { key.endsWith(it) && key.length - it.length >= 4 }
            if (suffix == null) key else key.removeSuffix(suffix)
        }
        .let { if (it.length > 5) it.replace(ADJECTIVE_ENDING, "") else it }

private fun ParsedDubbing.componentKeys(): Set<String> =
    text.split(SEPARATORS)
        .map { it.stripTypePrefixes().withoutDomain().compact().aliased() }
        .filter { it.isNotEmpty() }
        .toSet()

/**
 * Находит среди [candidates] (имена озвучек Yummy) ту, что совпадает с [name] (имя озвучки источника).
 * Подстроки не используются; на каждом шаге нужен ровно один кандидат, иначе берётся следующий шаг.
 * Шаги: то же написание → точный ключ → ключ без вложенных префиксов → мягкий ключ → набор компонентов.
 */
fun matchDubbingName(name: String, candidates: Collection<String>): String? =
    matchDubbingNames(name, candidates).firstOrNull()

/**
 * Как [matchDubbingName], но возвращает все озвучки-близнецы одной студии: «Animedia» и «AniMedia», «Озвучка X» и «Дубляж X»
 * (тот же точный ключ и тот же тип). Источник относится к каждой из них. Пустой список — совпадения нет или оно неоднозначно.
 */
fun matchDubbingNames(name: String, candidates: Collection<String>): List<String> {
    // Полное имя совпало буква в букву: «Озвучка X» и «Дубляж X» после снятия префикса неотличимы, а имя точное.
    candidates.firstOrNull { it == name }?.let { return listOf(it) }
    val target = name.parseDubbing()
    if (target.exactKey().isEmpty()) return emptyList()
    val parsed = candidates.distinct().map { it to it.parseDubbing() }

    fun find(pool: List<Pair<String, ParsedDubbing>>, exactOnly: Boolean): List<String> {
        fun unique(predicate: (ParsedDubbing) -> Boolean): List<String> =
            pool.filter { (_, p) -> predicate(p) }.map { it.first }.takeIf { it.size == 1 }.orEmpty()

        // Одна студия под разным написанием или типом: ключ тот же, значит берём всех.
        fun twins(predicate: (ParsedDubbing) -> Boolean): List<String> {
            val matches = pool.filter { (_, p) -> predicate(p) }
            if (matches.map { (_, p) -> p.exactKey() }.distinct().size != 1) return emptyList()
            return matches.map { it.first }
        }

        twins { it.text.trim() == target.text.trim() }.takeIf { it.isNotEmpty() }?.let { return it }
        twins { it.exactKey() == target.exactKey() }.takeIf { it.isNotEmpty() }?.let { return it }
        if (exactOnly) return emptyList()
        unique { it.baseKey() == target.baseKey() }.takeIf { it.isNotEmpty() }?.let { return it }
        target.looseKey().takeIf { it.isNotEmpty() }?.let { loose ->
            unique { it.looseKey() == loose }.takeIf { it.isNotEmpty() }?.let { return it }
        }
        val components = target.componentKeys()
        if (components.size > 1) {
            unique { it.componentKeys() == components }.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    find(parsed.filter { (_, p) -> p.kind == target.kind }, exactOnly = false).takeIf { it.isNotEmpty() }?.let { return it }
    // «Wakanim», «SubVost»: источник не назвал тип, а у Yummy такая команда есть только как субтитры.
    if (!target.explicitKind && target.kind == DubbingKind.VOICE) {
        return find(parsed.filter { (_, p) -> p.kind == DubbingKind.SUBTITLES }, exactOnly = true)
    }
    return emptyList()
}

/**
 * Подставляет [extras] каноническое имя озвучки Yummy из [base], если названия совпали по [matchDubbingNames].
 * Если у студии несколько озвучек-близнецов, видео источника копируется в каждую (копии получают свой id).
 * Видео без совпадения остаются со своим именем; ссылка источника (`iframeUrl`) не меняется.
 * Пустые тайминги пропуска берутся у видео [base] той же озвучки и серии (у источника-расширения
 * своих таймингов нет, а таймлайн релиза одной озвучки совпадает).
 */
fun mergeExtraDubbings(base: List<AnimeVideo>, extras: List<AnimeVideo>): List<AnimeVideo> {
    if (extras.isEmpty() || base.isEmpty()) return extras
    val names = base.map { it.dubbing.trim() }.filter { it.isNotEmpty() }.distinct()
    val cache = HashMap<String, List<String>>()
    val skipsByEpisode = HashMap<Pair<String, String>, AnimeVideoSkips>()
    base.forEach { video ->
        if (video.skips.opening == null && video.skips.ending == null) return@forEach
        skipsByEpisode.getOrPut(video.dubbing.trim() to video.episode.episodeGroupKey()) { video.skips }
    }
    return extras.flatMap { video ->
        val canonicals = cache.getOrPut(video.dubbing.trim()) { matchDubbingNames(video.dubbing.trim(), names) }
        val targets = canonicals.ifEmpty { listOf(video.dubbing) }
        targets.mapIndexed { index, canonical ->
            var copy = if (canonical == video.dubbing) video else video.copy(dubbing = canonical)
            // Первая копия сохраняет id (на нём держится прогресс), остальным нужен свой.
            if (index > 0) copy = copy.copy(id = -(("${video.id}|$canonical").hashCode() and Int.MAX_VALUE) - 1)
            if (copy.skips.opening != null || copy.skips.ending != null) return@mapIndexed copy
            val skips = skipsByEpisode[copy.dubbing.trim() to copy.episode.episodeGroupKey()]
            if (skips == null) copy else copy.copy(skips = skips)
        }
    }
}

/** Оставляет из [merged] (результат [mergeExtraDubbings]) только видео озвучек, которые есть в [base]. */
fun retainBaseDubbings(base: List<AnimeVideo>, merged: List<AnimeVideo>): List<AnimeVideo> {
    val names = base.map { it.dubbing.trim() }.toSet()
    return merged.filter { it.dubbing.trim() in names }
}
