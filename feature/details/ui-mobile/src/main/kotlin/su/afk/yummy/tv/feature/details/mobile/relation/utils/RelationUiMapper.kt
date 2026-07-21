package su.afk.yummy.tv.feature.details.mobile.relation.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.relation.model.RelationType

@StringRes
internal fun RelationType.labelRes(): Int = when (this) {
    RelationType.STUDIO -> R.string.details_mobile_relation_studio
    RelationType.DIRECTOR -> R.string.details_mobile_relation_director
    RelationType.GENRE -> R.string.details_mobile_relation_genre
}
