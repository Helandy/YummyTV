package su.afk.yummy.tv.feature.details.mobile.viewingorder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.feature.details.viewingorder.ViewingOrderState

class ViewingOrderMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ViewingOrderState.Event>()

    private val item = AnimeViewingOrderItem(
        animeId = 77,
        title = "Первая часть",
        relation = null,
        type = null,
        episodesCount = null,
        poster = null,
        year = null,
        rating = null,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: ViewingOrderState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ViewingOrderMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndEmptyMessage() {
        setScreen(ViewingOrderState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_viewing_order)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.empty_screen)).assertIsDisplayed()
    }

    @Test
    fun itemsShowUniverseHeaderAndTitle() {
        setScreen(ViewingOrderState.State(isLoading = false, items = persistentListOf(item)))

        composeRule.onNodeWithText(string(R.string.details_mobile_title_universe)).assertIsDisplayed()
        composeRule.onNodeWithText("Первая часть").assertIsDisplayed()
    }

    @Test
    fun itemClickSendsAnimeSelected() {
        setScreen(ViewingOrderState.State(isLoading = false, items = persistentListOf(item)))

        composeRule.onNodeWithText("Первая часть").performClick()

        assertEquals(listOf<ViewingOrderState.Event>(ViewingOrderState.Event.AnimeSelected(77)), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(ViewingOrderState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<ViewingOrderState.Event>(ViewingOrderState.Event.BackSelected), events)
    }
}
