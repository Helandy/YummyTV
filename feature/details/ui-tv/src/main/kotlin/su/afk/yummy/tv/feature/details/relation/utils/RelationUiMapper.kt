package su.afk.yummy.tv.feature.details.relation.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.relation.model.RelationType

@StringRes
internal fun RelationType.labelRes(): Int = when (this) {
    RelationType.STUDIO -> R.string.details_relation_studio
    RelationType.DIRECTOR -> R.string.details_relation_director
    RelationType.GENRE -> R.string.details_relation_genre
}
