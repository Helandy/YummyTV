package su.afk.yummy.tv.feature.pages.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.pages.model.SitePage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.feature.pages.SitePagesState
import su.afk.yummy.tv.feature.pages.presentation.R

class SitePagesMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SitePagesState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: SitePagesState.State = SitePagesState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SitePagesMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun listShowsTitleAndPageRows() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.site_pages_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.site_page_faq)).assertIsDisplayed()
    }

    @Test
    fun rowClickSendsPageSelected() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.site_page_faq)).performClick()

        assertEquals(listOf<SitePagesState.Event>(SitePagesState.Event.PageSelected(SitePageType.FAQ)), events)
    }

    @Test
    fun loadingHidesPageList() {
        setScreen(SitePagesState.State(selectedType = SitePageType.FAQ, loading = true))

        composeRule.onNodeWithText(string(R.string.site_page_rules)).assertDoesNotExist()
    }

    @Test
    fun loadedPageShowsText() {
        setScreen(
            SitePagesState.State(
                selectedType = SitePageType.FAQ,
                page = SitePage(title = "FAQ", text = "Тестовый текст страницы"),
            ),
        )

        composeRule.onNodeWithText("Тестовый текст страницы").assertIsDisplayed()
    }

    @Test
    fun fallbackShowsMessageAndRetrySendsRetrySelected() {
        setScreen(SitePagesState.State(selectedType = SitePageType.FAQ))

        composeRule.onNodeWithText(string(R.string.site_page_fallback)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<SitePagesState.Event>(SitePagesState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<SitePagesState.Event>(SitePagesState.Event.BackSelected), events)
    }
}
