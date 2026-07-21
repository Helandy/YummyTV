package su.afk.yummy.tv.domain.posts.model

enum class PostVote(val action: Int) { LIKE(1), DISLIKE(-1), NONE(0) }
