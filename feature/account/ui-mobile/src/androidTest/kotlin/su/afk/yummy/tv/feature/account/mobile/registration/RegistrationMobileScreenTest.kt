package su.afk.yummy.tv.feature.account.mobile.registration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.registration.RegistrationState

class RegistrationMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RegistrationState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    // в русской локали заголовок и кнопка называются одинаково, поэтому кнопку ищем по кликабельности
    private fun registerButton() = composeRule.onNode(
        hasText(string(R.string.account_mobile_register)) and hasClickAction(),
    )

    private fun setScreen(state: RegistrationState.State = RegistrationState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                RegistrationMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndRegisterButton() {
        setScreen()

        composeRule.onAllNodesWithText(string(R.string.account_registration_title))[0].assertIsDisplayed()
        registerButton().assertIsDisplayed()
    }

    @Test
    fun typingEmailSendsEmailChanged() {
        setScreen()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("a@b.c")

        assertTrue(RegistrationState.Event.EmailChanged("a@b.c") in events)
    }

    @Test
    fun typingUsernameSendsUsernameChanged() {
        setScreen()

        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("nick")

        assertTrue(RegistrationState.Event.UsernameChanged("nick") in events)
    }

    @Test
    fun typingPasswordSendsPasswordChanged() {
        setScreen()

        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("secret1")

        assertTrue(RegistrationState.Event.PasswordChanged("secret1") in events)
    }

    @Test
    fun registerButtonSendsRegisterSelected() {
        setScreen()

        registerButton().performClick()

        assertTrue(RegistrationState.Event.RegisterSelected in events)
    }

    @Test
    fun loadingReplacesRegisterLabelWithProgress() {
        setScreen(RegistrationState.State(isLoading = true))

        registerButton().assertDoesNotExist()
    }

    @Test
    fun errorIsShown() {
        setScreen(RegistrationState.State(error = AccountUiError.INVALID_EMAIL))

        composeRule.onNodeWithText(string(R.string.account_error_invalid_email))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun successShowsBackToLogin() {
        setScreen(RegistrationState.State(isSuccess = true))

        composeRule.onNodeWithText(string(R.string.account_registration_success_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.account_registration_back_to_login)).performClick()

        assertTrue(RegistrationState.Event.BackSelected in events)
    }
}
