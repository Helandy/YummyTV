package su.afk.yummy.tv.feature.reviews.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.feature.reviews.presentation.R

/** Подпись сортировки рецензий. */
@StringRes
fun ReviewSort.labelRes(): Int = when (this) {
    ReviewSort.NEW -> R.string.reviews_new
    ReviewSort.OLD -> R.string.reviews_old
    ReviewSort.TOP -> R.string.reviews_top
}
