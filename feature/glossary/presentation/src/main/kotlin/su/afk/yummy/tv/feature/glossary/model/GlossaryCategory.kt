package su.afk.yummy.tv.feature.glossary.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.glossary.presentation.R

/** Раздел глоссария; порядок объявления задаёт порядок показа на экране. */
enum class GlossaryCategory(@param:StringRes val title: Int) {
    SLANG(R.string.glossary_category_slang),
    GENRES(R.string.glossary_category_genres),
    PRODUCTION(R.string.glossary_category_production),
    VOICE(R.string.glossary_category_voice),
}
