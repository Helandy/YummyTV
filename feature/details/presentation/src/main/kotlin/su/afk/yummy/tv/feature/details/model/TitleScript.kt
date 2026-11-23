package su.afk.yummy.tv.feature.details.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.details.presentation.R

/** API не возвращает язык названия: группа определяется по письменности. */
enum class TitleScript(@StringRes val labelRes: Int) {
    Alternative(R.string.details_mobile_titles_alternative),
    English(R.string.details_mobile_titles_english),
    Japanese(R.string.details_mobile_titles_japanese),
}
