package su.afk.yummy.tv.domain.reviews.model

enum class ReviewVote(val apiValue: Int) { LIKE(1), NONE(0), DISLIKE(-1) }
