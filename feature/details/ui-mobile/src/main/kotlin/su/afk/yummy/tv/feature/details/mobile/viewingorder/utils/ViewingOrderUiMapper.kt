package su.afk.yummy.tv.feature.details.mobile.viewingorder.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun AnimeViewingOrderItem.mobileMeta(): String = listOfNotNull(
    type,
    episodesCount?.let { stringResource(R.string.details_mobile_episodes_count_short, it) },
).joinToString(" · ")
