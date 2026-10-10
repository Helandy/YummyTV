package su.afk.yummy.tv.feature.top.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.presentation.R

/** Подпись типа топа: TV, фильм, ONA. */
@StringRes
fun AnimeTopType.labelRes(): Int = when (this) {
    AnimeTopType.TV -> R.string.top_type_tv
    AnimeTopType.MOVIE -> R.string.top_type_movie
    AnimeTopType.ONA -> R.string.top_type_ona
}
