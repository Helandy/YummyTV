package su.afk.yummy.tv.feature.community.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.scroll.PinToEndOnViewportChange
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.feature.community.CommunityState
import su.afk.yummy.tv.feature.community.mobile.view.CommunityNewsSection
import su.afk.yummy.tv.feature.community.mobile.view.CommunitySectionGrid
import su.afk.yummy.tv.feature.community.mobile.view.CommunityVideosSection
import su.afk.yummy.tv.feature.community.model.CommunitySection
import su.afk.yummy.tv.feature.community.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMobileScreen(
    state: CommunityState.State,
    effect: Flow<CommunityState.Effect>,
    onEvent: (CommunityState.Event) -> Unit,
) {
    BaseScreen(isScroll = false) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(CommunityState.Event.RefreshSelected) },
            modifier = Modifier.fillMaxSize(),
        ) {
            val listState = rememberLazyListState()
            listState.PinToEndOnViewportChange()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = MobileBottomBarDefaults.rootContentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "sections") {
                    CommunitySectionGrid(
                        sections = CommunitySection.entries,
                        onSectionSelected = { onEvent(CommunityState.Event.SectionSelected(it)) },
                    )
                }

                if (state.news.isNotEmpty()) {
                    item(key = "news") {
                        CommunityNewsSection(
                            title = stringResource(R.string.community_section_news),
                            allLabel = stringResource(R.string.community_all),
                            news = state.news,
                            onNewsSelected = { onEvent(CommunityState.Event.NewsSelected(it.id)) },
                            onAllSelected = { onEvent(CommunityState.Event.AllNewsSelected) },
                        )
                    }
                } else if (state.isNewsLoading) {
                    item(key = "news_loading") { MobileSectionLoading(minHeight = 120.dp) }
                }
                state.newsError?.let { message ->
                    item(key = "news_error") {
                        MobileMessage(
                            title = message.asString(),
                            actionLabel = stringResource(CoreR.string.common_retry),
                            onAction = { onEvent(CommunityState.Event.NewsRetrySelected) },
                            fillMaxSize = false,
                        )
                    }
                }

                if (state.videos.isNotEmpty()) {
                    item(key = "videos") {
                        CommunityVideosSection(
                            title = stringResource(R.string.community_videos_title),
                            allLabel = stringResource(R.string.community_all),
                            videos = state.videos,
                            onVideoSelected = { onEvent(CommunityState.Event.VideoSelected(it.id)) },
                            onAllSelected = { onEvent(CommunityState.Event.AllVideosSelected) },
                        )
                    }
                } else if (state.isVideosLoading) {
                    item(key = "videos_loading") { MobileSectionLoading(minHeight = 120.dp) }
                }
                state.videosError?.let { message ->
                    item(key = "videos_error") {
                        MobileMessage(
                            title = message.asString(),
                            actionLabel = stringResource(CoreR.string.common_retry),
                            onAction = { onEvent(CommunityState.Event.VideosRetrySelected) },
                            fillMaxSize = false,
                        )
                    }
                }
            }
        }
    }
}
