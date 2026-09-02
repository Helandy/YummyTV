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
) {
    fun encode(): String = "$videoId|$timeSeconds|$durationSeconds"

    companion object {
        fun decode(payload: String): MarkWatchedPayload {
            val (videoId, timeSeconds, durationSeconds) = payload.split("|")
            return MarkWatchedPayload(videoId.toInt(), timeSeconds.toInt(), durationSeconds.toInt())
        }
    }
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
