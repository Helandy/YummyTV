package su.afk.yummy.tv.feature.pages.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.feature.pages.mobile.R

@Composable
internal fun SitePageType.title(): String = stringResource(
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
