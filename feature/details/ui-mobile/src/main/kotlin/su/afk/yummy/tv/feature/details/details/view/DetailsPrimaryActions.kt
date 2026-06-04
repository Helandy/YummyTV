package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.utils.libraryLabel
import su.afk.yummy.tv.feature.details.details.utils.watchLabel
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun DetailsPrimaryActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchLabel = state.watchLabel(details)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onWatchSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = watchLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onLibraryToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            ) {
                Icon(
                    imageVector = if (state.isInLibrary) {
                        Icons.AutoMirrored.Filled.PlaylistAddCheck
                    } else {
                        Icons.AutoMirrored.Filled.PlaylistAdd
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.libraryLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (state.isFavorite) R.string.details_mobile_favorite_on
                        else R.string.details_mobile_favorite_off,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
