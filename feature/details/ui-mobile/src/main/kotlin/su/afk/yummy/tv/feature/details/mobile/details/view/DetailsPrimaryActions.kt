package su.afk.yummy.tv.feature.details.mobile.details.view

import su.afk.yummy.tv.core.designsystem.seasonal.seasonalButtonDecor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.mobile.details.utils.libraryLabel
import su.afk.yummy.tv.feature.details.mobile.details.utils.watchLabel
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.utils.statusColor
import su.afk.yummy.tv.feature.details.utils.statusIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            enabled = !state.isWatchDataLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .seasonalButtonDecor()
                .testTag("watch_button"),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = watchLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val libraryStatusColor = state.libraryList?.takeIf { state.isInLibrary }?.statusColor()
        ButtonGroup(
            overflowIndicator = {},
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = onLibraryToggle,
                        interactionSource = interactionSource,
                        colors = if (libraryStatusColor != null) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = libraryStatusColor.copy(alpha = 0.18f),
                                contentColor = libraryStatusColor,
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactionSource)
                            .height(46.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isInLibrary) {
                                (state.libraryList ?: UserAnimeList.WATCHING).statusIcon()
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
                },
                menuContent = {},
            )
            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = onFavoriteToggle,
                        interactionSource = interactionSource,
                        colors = if (state.isFavorite) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = YummySemanticColors.StatusFavorite.copy(alpha = 0.18f),
                                contentColor = YummySemanticColors.StatusFavorite,
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactionSource)
                            .height(46.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.details_add_favorite),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                menuContent = {},
            )
        }
    }
}
