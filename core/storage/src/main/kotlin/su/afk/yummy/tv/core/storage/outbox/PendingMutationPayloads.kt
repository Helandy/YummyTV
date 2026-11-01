package su.afk.yummy.tv.core.storage.outbox

/**
 * Плоские `|`-разделённые кодировщики для [PendingMutationEntry.payloadJson] (имя поля осталось
 * от первоначального плана хранить JSON, но для нескольких примитивных полей на мутацию формат
 * не важен — а лишний парсер не нужен). Кодирует enqueue-сторона (presentation-хендлеры), декодирует
 * воркер синхронизации — оба используют один и тот же формат отсюда, так что рассинхрона не будет.
 */
data class MarkWatchedPayload(
    val videoId: Int,
    val timeSeconds: Int,
    val durationSeconds: Int,
    val times: List<Int> = emptyList(),
) {
    /** Секунды пишутся диапазонами (`0-120,300-310`): серия на 20 минут не раздувает запись в тысячи чисел. */
    fun encode(): String = "$videoId|$timeSeconds|$durationSeconds|${times.toSecondRanges()}"

    companion object {
        fun decode(payload: String): MarkWatchedPayload {
            // Записи старого формата (без секунд) состоят из трёх полей.
            val parts = payload.split("|")
            return MarkWatchedPayload(
                videoId = parts[0].toInt(),
                timeSeconds = parts[1].toInt(),
                durationSeconds = parts[2].toInt(),
                times = parts.getOrNull(3).orEmpty().fromSecondRanges(),
            )
        }
    }
}

internal fun List<Int>.toSecondRanges(): String {
    val seconds = distinct().sorted()
    if (seconds.isEmpty()) return ""
    val ranges = mutableListOf<String>()
    var start = seconds.first()
    var previous = start
    for (second in seconds.drop(1)) {
        if (second == previous + 1) {
            previous = second
            continue
        }
        ranges += if (start == previous) "$start" else "$start-$previous"
        start = second
        previous = second
    }
    ranges += if (start == previous) "$start" else "$start-$previous"
    return ranges.joinToString(",")
}

internal fun String.fromSecondRanges(): List<Int> =
    split(",").filter { it.isNotBlank() }.flatMap { range ->
        val bounds = range.split("-")
        if (bounds.size == 2) (bounds[0].toInt()..bounds[1].toInt()).toList() else listOf(bounds[0].toInt())
    }

data class RemoveWatchedPayload(val videoIds: List<Int>) {
    fun encode(): String = videoIds.joinToString(",")

    companion object {
        fun decode(payload: String): RemoveWatchedPayload =
            RemoveWatchedPayload(payload.split(",").filter { it.isNotBlank() }.map { it.toInt() })
    }
}

data class SetListPayload(val animeId: Int, val listId: Int) {
    fun encode(): String = "$animeId|$listId"

    companion object {
        fun decode(payload: String): SetListPayload {
            val (animeId, listId) = payload.split("|")
            return SetListPayload(animeId.toInt(), listId.toInt())
        }
    }
}

data class AnimeIdPayload(val animeId: Int) {
    fun encode(): String = animeId.toString()

    companion object {
        fun decode(payload: String): AnimeIdPayload = AnimeIdPayload(payload.toInt())
    }
}

data class SetFavoritePayload(val animeId: Int, val favorite: Boolean) {
    fun encode(): String = "$animeId|$favorite"

    companion object {
        fun decode(payload: String): SetFavoritePayload {
            val (animeId, favorite) = payload.split("|")
            return SetFavoritePayload(animeId.toInt(), favorite.toBoolean())
        }
    }
}

data class SetRatingPayload(val animeId: Int, val rating: Int) {
    fun encode(): String = "$animeId|$rating"

    companion object {
        fun decode(payload: String): SetRatingPayload {
            val (animeId, rating) = payload.split("|")
            return SetRatingPayload(animeId.toInt(), rating.toInt())
        }
    }
}

data class VoteReviewPayload(val reviewId: Int, val voteApiValue: Int) {
    fun encode(): String = "$reviewId|$voteApiValue"

    companion object {
        fun decode(payload: String): VoteReviewPayload {
            val (reviewId, voteApiValue) = payload.split("|")
            return VoteReviewPayload(reviewId.toInt(), voteApiValue.toInt())
        }
    }
}
