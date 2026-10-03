package su.afk.yummy.tv.core.model.mutation

/** Мутация аккаунта, которую нужно дослать на сервер, когда вернётся сеть. */
sealed interface PendingMutation {
    data class MarkWatched(
        val videoId: Int,
        val timeSeconds: Int,
        val durationSeconds: Int,
    ) : PendingMutation

    data class RemoveWatched(val videoIds: List<Int>) : PendingMutation

    data class SetList(val animeId: Int, val listId: Int) : PendingMutation

    data class RemoveList(val animeId: Int) : PendingMutation

    data class SetFavorite(val animeId: Int, val favorite: Boolean) : PendingMutation

    data class SetRating(val animeId: Int, val rating: Int) : PendingMutation

    data class DeleteRating(val animeId: Int) : PendingMutation

    data class VoteReview(val reviewId: Int, val voteApiValue: Int) : PendingMutation
}
