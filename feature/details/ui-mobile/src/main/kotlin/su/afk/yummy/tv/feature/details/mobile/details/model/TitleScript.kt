package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.details.presentation.R

/** Группа альтернативного названия: API язык не отдаёт, группа определяется по символам. */
internal enum class TitleScript(@StringRes val labelRes: Int) {
    Alternative(R.string.details_mobile_titles_alternative),
    English(R.string.details_mobile_titles_english),
    Japanese(R.string.details_mobile_titles_japanese),
}
