package su.afk.yummy.tv.feature.bloggers.mobile.utils

import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.mobile.R

internal fun BloggerVideoSort.labelRes() = when (this) {
    BloggerVideoSort.NEW -> R.string.blogger_videos_sort_new
    BloggerVideoSort.TOP -> R.string.blogger_videos_sort_top
    BloggerVideoSort.OLD -> R.string.blogger_videos_sort_old
}
