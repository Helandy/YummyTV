package su.afk.yummy.tv.feature.collection.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.collection.model.CollectionSort
import su.afk.yummy.tv.feature.collection.presentation.R

/** Подпись сортировки каталога коллекций. */
@StringRes
fun CollectionSort.labelRes(): Int = when (this) {
    CollectionSort.NEW -> R.string.collection_sort_new
    CollectionSort.BEST -> R.string.collection_sort_best
    CollectionSort.OLD -> R.string.collection_sort_old
}
