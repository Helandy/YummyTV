package su.afk.yummy.tv.feature.watchlater.mobile

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
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.feature.watchlater.WatchLaterState
import su.afk.yummy.tv.feature.watchlater.presentation.R

class WatchLaterMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<WatchLaterState.Event>()

    private val item = WatchLaterItem(
        animeId = 42,
        episode = "7",
        animeTitle = "Тестовое аниме",
        posterUrl = "",
        screenshotUrl = "",
        addedAt = 0L,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: WatchLaterState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                WatchLaterMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun emptyStateShowsMessage() {
        setScreen(WatchLaterState.State())

        composeRule.onNodeWithText(string(R.string.watch_later_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.watch_later_empty)).assertIsDisplayed()
    }

    @Test
    fun itemShowsTitle() {
        setScreen(WatchLaterState.State(items = persistentListOf(item)))

        composeRule.onNodeWithText("Тестовое аниме").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.watch_later_empty)).assertDoesNotExist()
    }

    @Test
    fun itemClickSendsItemSelected() {
        setScreen(WatchLaterState.State(items = persistentListOf(item)))

        composeRule.onNodeWithText("Тестовое аниме").performClick()

        assertEquals(listOf<WatchLaterState.Event>(WatchLaterState.Event.ItemSelected(42, "7")), events)
    }

    @Test
    fun removeSendsRemoveSelected() {
        setScreen(WatchLaterState.State(items = persistentListOf(item)))

        composeRule.onNodeWithContentDescription(string(R.string.watch_later_remove)).performClick()

        assertEquals(listOf<WatchLaterState.Event>(WatchLaterState.Event.RemoveSelected(42, "7")), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(WatchLaterState.State())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<WatchLaterState.Event>(WatchLaterState.Event.BackSelected), events)
    }
}
