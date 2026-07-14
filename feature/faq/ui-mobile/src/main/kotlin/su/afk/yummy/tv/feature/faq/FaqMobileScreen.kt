package su.afk.yummy.tv.feature.faq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.feature.faq.mobile.R
import su.afk.yummy.tv.feature.faq.view.FaqExpandableItem
import su.afk.yummy.tv.feature.faq.view.FaqFeedbackButton

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FaqMobileScreenDefaultPreview() =
    ScreenPreviewTheme { FaqMobileScreen {} }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FaqMobileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.faq_repository_url)
    val questions = listOf(
        R.string.faq_who_am_i_title to R.string.faq_who_am_i_answer,
        R.string.faq_why_this_app_title to R.string.faq_why_this_app_answer,
        R.string.faq_why_not_all_cases_title to R.string.faq_why_not_all_cases_answer,
        R.string.faq_why_errors_title to R.string.faq_why_errors_answer,
        R.string.faq_missing_site_feature_title to R.string.faq_missing_site_feature_answer,
        R.string.faq_unsupported_player_title to R.string.faq_unsupported_player_answer,
        R.string.faq_external_player_title to R.string.faq_external_player_answer,
        R.string.faq_not_official_title to R.string.faq_not_official_answer,
        R.string.faq_why_feedback_title to R.string.faq_why_feedback_answer,
    )
    var expandedItems by remember { mutableStateOf(emptySet<Int>()) }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.faq_title),
                onBack = onBack,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            questions.forEachIndexed { index, (titleRes, answerRes) ->
                item(key = titleRes) {
                    FaqExpandableItem(
                        title = stringResource(titleRes),
                        answer = stringResource(answerRes),
                        expanded = index in expandedItems,
                        onClick = {
                            expandedItems = if (index in expandedItems) {
                                expandedItems - index
                            } else {
                                expandedItems + index
                            }
                        },
                    )
                }
            }
            item(key = "feedback") {
                FaqFeedbackButton(
                    text = stringResource(R.string.faq_feedback_button),
                    onClick = { context.openExternalUri(repositoryUrl) },
                )
            }
        }
    }
}
