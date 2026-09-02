package su.afk.yummy.tv.core.storage.outbox

/** [PendingMutationEntry.type] values — kept as plain strings so the DB survives enum renames. */
object PendingMutationTypes {
    const val MARK_WATCHED = "mark_watched"
    const val REMOVE_WATCHED = "remove_watched"
    const val SET_LIST = "set_list"
    const val REMOVE_LIST = "remove_list"
    const val SET_FAVORITE = "set_favorite"
    const val SET_RATING = "set_rating"
    const val DELETE_RATING = "delete_rating"
    const val VOTE_REVIEW = "vote_review"
}
