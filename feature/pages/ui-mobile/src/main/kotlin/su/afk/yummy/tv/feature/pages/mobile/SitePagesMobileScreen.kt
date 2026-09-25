package su.afk.yummy.tv.feature.pages.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.feature.pages.SitePagesState
import su.afk.yummy.tv.feature.pages.mobile.utils.title
import su.afk.yummy.tv.feature.pages.mobile.view.SitePageRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePagesMobileScreen(
    state: SitePagesState.State,
    effect: Flow<SitePagesState.Effect>,
    onEvent: (SitePagesState.Event) -> Unit,
) {
    val loadedPage = state.page
    BackHandler(enabled = state.selectedType != null) {
        onEvent(SitePagesState.Event.BackSelected)
    }
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = state.selectedType?.title() ?: stringResource(R.string.site_pages_title),
                onBack = { onEvent(SitePagesState.Event.BackSelected) },
            )
        },
    ) {
        when {
            state.selectedType == null -> LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(12.dp),
            ) {
                items(SitePageType.entries, key = SitePageType::apiValue) { type ->
                    SitePageRow(type.title()) { onEvent(SitePagesState.Event.PageSelected(type)) }
                }
            }

            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            loadedPage != null -> LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(20.dp),
            ) {
                item { Text(loadedPage.text) }
            }

            else -> MobileMessage(
                title = stringResource(R.string.site_page_fallback),
                actionLabel = stringResource(R.string.site_page_retry),
                onAction = { onEvent(SitePagesState.Event.RetrySelected) },
            )
        }
    }
}
