package su.afk.yummy.tv.feature.posts.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.feature.posts.presentation.R

/** Подпись сортировки постов. */
@StringRes
fun PostSort.labelRes(): Int = when (this) {
    PostSort.NEW -> R.string.posts_new
    PostSort.OLD -> R.string.posts_old
    PostSort.BEST -> R.string.posts_best
}
