package su.afk.yummy.tv.feature.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.feature.pages.mobile.R
import su.afk.yummy.tv.feature.pages.view.SitePageRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePagesMobileScreen(
    state: SitePagesState.State,
    effect: Flow<SitePagesState.Effect>,
    onEvent: (SitePagesState.Event) -> Unit,
) {
    val loadedPage = state.page
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.selectedType?.title() ?: stringResource(R.string.site_pages_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SitePagesState.Event.BackSelected) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.selectedType == null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
            ) {
                items(SitePageType.entries, key = SitePageType::apiValue) { type ->
                    SitePageRow(type.title()) { onEvent(SitePagesState.Event.PageSelected(type)) }
                }
            }

            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            loadedPage != null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
            ) {
                item { Text(loadedPage.text) }
            }

            else -> MobileMessage(
                title = stringResource(R.string.site_page_fallback),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.site_page_retry),
                onAction = { onEvent(SitePagesState.Event.RetrySelected) },
            )
        }
    }
}

@Composable
private fun SitePageType.title(): String = stringResource(
    when (this) {
        SitePageType.FAQ -> R.string.site_page_faq
        SitePageType.ABOUT_SITE -> R.string.site_page_about
        SitePageType.RULES -> R.string.site_page_rules
        SitePageType.REVIEW_RULES -> R.string.site_page_review_rules
        SitePageType.COLLECTIONS_RULES -> R.string.site_page_collections_rules
        SitePageType.POST_RULES -> R.string.site_page_post_rules
        SitePageType.EDIT_RULES -> R.string.site_page_edit_rules
        SitePageType.PRIVACY -> R.string.site_page_privacy
        SitePageType.COPYRIGHT -> R.string.site_page_copyright
        SitePageType.WANT_TO_HELP -> R.string.site_page_help
    }
)
