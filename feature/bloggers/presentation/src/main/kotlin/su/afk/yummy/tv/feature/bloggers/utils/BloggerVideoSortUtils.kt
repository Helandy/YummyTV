package su.afk.yummy.tv.feature.bloggers.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.presentation.R

/** Подпись сортировки видео блогеров. */
@StringRes
fun BloggerVideoSort.labelRes(): Int = when (this) {
    BloggerVideoSort.NEW -> R.string.blogger_videos_sort_new
    BloggerVideoSort.TOP -> R.string.blogger_videos_sort_top
    BloggerVideoSort.OLD -> R.string.blogger_videos_sort_old
}
