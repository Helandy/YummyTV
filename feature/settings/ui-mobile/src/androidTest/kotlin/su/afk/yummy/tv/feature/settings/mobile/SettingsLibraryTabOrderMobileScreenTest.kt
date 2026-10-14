package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class SettingsLibraryTabOrderMobileScreenTest {
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
                SettingsLibraryTabOrderMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun opensWithScreenOpenedEventAndTitle() {
        // название есть и в топ-баре, и в заголовке секции
        composeRule.onAllNodesWithText(string(R.string.settings_library_tabs_order)).assertCountEquals(2)
        assertTrue(SettingsState.Event.LibraryTabOrderScreenOpened in events)
    }

    @Test
    fun moveDownOnSecondRowSendsMoveEvent() {
        events.clear()

        // первая вкладка закреплена, поэтому двигаем вторую
        composeRule.onAllNodesWithContentDescription(string(R.string.settings_details_button_move_down))[1]
            .performClick()

        assertEquals(
            listOf<SettingsState.Event>(
                SettingsState.Event.LibraryTabMoved(state.libraryTabOrder[1], DetailsButtonMoveDirection.DOWN),
            ),
            events,
        )
    }

    @Test
    fun resetSendsLibraryTabOrderReset() {
        events.clear()

        composeRule.onNodeWithText(string(R.string.settings_library_tabs_reset)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.LibraryTabOrderReset), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        events.clear()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.BackSelected), events)
    }
}
