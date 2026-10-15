package su.afk.yummy.tv.feature.home.mobile.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterGrid
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.home.model.bestUrl
import su.afk.yummy.tv.feature.home.recommendations.HomeRecommendationsState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeRecommendationsMobileScreen(
    state: HomeRecommendationsState.State,
    effect: Flow<HomeRecommendationsState.Effect>,
    onEvent: (HomeRecommendationsState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        isLoading = state.isLoading,
        customTopBar = {
            MobileTopBar(
                title = state.title,
                onBack = { onEvent(HomeRecommendationsState.Event.BackSelected) },
            )
        },
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(bottom = MobileBottomBarDefaults.contentBottomPadding),
        ) {
            items(state.items, key = { lazyKey("homerecommendation", it.id) }) { item ->
                MobilePosterCard(
                    title = item.title,
                    posterUrl = item.poster.bestUrl(),
                    animeId = item.id,
                    rating = item.rating,
                    posterOverlay = {
                        item.year?.let { year ->
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.inverseOnSurface,
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    },
                    onClick = { onEvent(HomeRecommendationsState.Event.AnimeSelected(item.id)) },
                )
            }
        }
    }
}
