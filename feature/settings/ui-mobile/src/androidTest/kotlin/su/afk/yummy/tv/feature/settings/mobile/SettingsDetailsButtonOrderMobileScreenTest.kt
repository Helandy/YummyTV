package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.presentation.R

class SettingsDetailsButtonOrderMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsState.Event>()
    private val state = SettingsState.State()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Before
    fun setUp() {
        composeRule.setContent {
            ScreenPreviewTheme {
                SettingsDetailsButtonOrderMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun opensWithScreenOpenedEventAndTitle() {
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.settings_details_buttons_order)).assertIsDisplayed()
        assertTrue(SettingsState.Event.DetailsButtonOrderScreenOpened in events)
    }

    @Test
    fun moveDownOnFirstRowSendsMoveEvent() {
        composeRule.waitForIdle()
        events.clear()

        composeRule.onAllNodesWithContentDescription(string(R.string.settings_details_button_move_down))[0]
            .performClick()

        assertEquals(
            listOf<SettingsState.Event>(
                SettingsState.Event.DetailsButtonMoved(state.detailsButtonOrder.first(), DetailsButtonMoveDirection.DOWN),
            ),
            events,
        )
    }

    @Test
    fun resetSendsDetailsButtonOrderReset() {
        composeRule.waitForIdle()
        events.clear()

        composeRule.onNodeWithText(string(R.string.settings_details_buttons_reset)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.DetailsButtonOrderReset), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        composeRule.waitForIdle()
        events.clear()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.BackSelected), events)
    }
}
