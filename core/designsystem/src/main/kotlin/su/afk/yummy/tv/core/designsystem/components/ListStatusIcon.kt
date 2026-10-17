package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.anime.CardListStatus

/** Иконка пользовательского списка — одна на все места, где список показывается. */
fun CardListStatus.icon(): ImageVector = when (this) {
    CardListStatus.WATCHING -> Icons.Filled.Visibility
    CardListStatus.PLANNED -> Icons.Filled.Cloud
    CardListStatus.COMPLETED -> Icons.Filled.SportsScore
    CardListStatus.POSTPONED -> Icons.Filled.HourglassEmpty
    CardListStatus.DROPPED -> Icons.Filled.VisibilityOff
}
