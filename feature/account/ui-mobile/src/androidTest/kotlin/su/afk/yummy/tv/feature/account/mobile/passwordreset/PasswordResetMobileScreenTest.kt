package su.afk.yummy.tv.feature.account.mobile.passwordreset

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.passwordreset.PasswordResetState
import su.afk.yummy.tv.feature.account.presentation.R

class PasswordResetMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PasswordResetState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: PasswordResetState.State = PasswordResetState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                PasswordResetMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsFormByDefault() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.password_reset_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.password_reset_description)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.password_reset_send)).assertIsDisplayed()
    }

    @Test
    fun typingSendsEmailChanged() {
        setScreen()

        composeRule.onNode(hasSetTextAction()).performTextInput("a@b.c")

        assertEquals(listOf<PasswordResetState.Event>(PasswordResetState.Event.EmailChanged("a@b.c")), events)
    }

    @Test
    fun sendButtonSendsSubmitSelected() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.password_reset_send)).performClick()

        assertEquals(listOf<PasswordResetState.Event>(PasswordResetState.Event.SubmitSelected), events)
    }

    @Test
    fun validationErrorIsShown() {
        setScreen(PasswordResetState.State(validationError = true))

        composeRule.onNodeWithText(string(R.string.password_reset_invalid_email)).assertIsDisplayed()
    }

    @Test
    fun requestErrorIsShown() {
        setScreen(PasswordResetState.State(requestError = true))

        composeRule.onNodeWithText(string(R.string.password_reset_error)).assertIsDisplayed()
    }

    @Test
    fun loadingDisablesField() {
        setScreen(PasswordResetState.State(isLoading = true))

        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.password_reset_send)).assertDoesNotExist()
    }

    @Test
    fun successReplacesFormWithMessage() {
        setScreen(PasswordResetState.State(isSuccess = true))

        composeRule.onNodeWithText(string(R.string.password_reset_success)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.password_reset_send)).assertDoesNotExist()
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<PasswordResetState.Event>(PasswordResetState.Event.BackSelected), events)
    }
}
