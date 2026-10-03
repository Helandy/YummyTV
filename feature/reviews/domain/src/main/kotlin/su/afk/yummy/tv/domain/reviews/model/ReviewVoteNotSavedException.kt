package su.afk.yummy.tv.domain.reviews.model

/** Сервер не принял голос за рецензию. */
class ReviewVoteNotSavedException : Exception("Vote was not saved")
