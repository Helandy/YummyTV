package su.afk.yummy.tv.feature.account.mobile.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class AccountMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<AccountState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun signedOut() = AccountState.State(isSessionResolved = true, isSignedIn = false)

    private fun signedIn() = AccountState.State(
        isSessionResolved = true,
        isSignedIn = true,
        userId = 12,
        nickname = "ТестовыйАккаунт",
    )

    private fun setScreen(state: AccountState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                AccountMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun screenShownEventIsSentOnOpen() {
        setScreen(signedOut())
        composeRule.waitForIdle()

        assertTrue(AccountState.Event.ScreenShown in events)
    }

    @Test
    fun unresolvedSessionHidesLoginForm() {
        setScreen(AccountState.State(isSessionResolved = false))

        composeRule.onNodeWithText(string(R.string.account_forgot_password)).assertDoesNotExist()
    }

    @Test
    fun signedOutShowsLoginForm() {
        setScreen(signedOut())

        composeRule.onNodeWithText(string(R.string.account_forgot_password)).assertIsDisplayed()
    }

    @Test
    fun typingLoginSendsLoginChanged() {
        setScreen(signedOut())

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("user@mail.ru")

        assertTrue(AccountState.Event.LoginChanged("user@mail.ru") in events)
    }

    @Test
    fun signInButtonSendsLoginSelected() {
        setScreen(signedOut())

        composeRule
            .onNode(hasText(string(DesignSystemR.string.common_sign_in)) and hasClickAction())
            .performScrollTo()
            .performClick()

        assertTrue(AccountState.Event.LoginSelected in events)
    }

    @Test
    fun registerButtonSendsRegistrationSelected() {
        setScreen(signedOut())

        composeRule
            .onNode(hasText(string(R.string.account_mobile_register)) and hasClickAction())
            .performScrollTo()
            .performClick()

        assertTrue(AccountState.Event.RegistrationSelected in events)
    }

    @Test
    fun forgotPasswordSendsPasswordResetSelected() {
        setScreen(signedOut())

        composeRule.onNodeWithText(string(R.string.account_forgot_password)).performScrollTo().performClick()

        assertTrue(AccountState.Event.PasswordResetSelected in events)
    }

    @Test
    fun quickActionSettingsSendsSettingsSelected() {
        setScreen(signedOut())

        composeRule.onNodeWithText(string(R.string.account_settings)).performClick()

        assertTrue(AccountState.Event.SettingsSelected in events)
    }

    @Test
    fun quickActionFaqSendsFaqSelected() {
        setScreen(signedOut())

        composeRule.onNodeWithText(string(R.string.account_faq)).performClick()

        assertTrue(AccountState.Event.FaqSelected in events)
    }

    @Test
    fun signedInShowsNicknameAndMessagesAction() {
        setScreen(signedIn())

        composeRule.onNodeWithText("ТестовыйАккаунт").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.account_notification_type_message)).assertIsDisplayed()
    }

    @Test
    fun editProfileSendsProfileEditSelected() {
        setScreen(signedIn())

        composeRule.onNodeWithContentDescription(string(R.string.account_edit_profile)).performClick()

        assertTrue(AccountState.Event.ProfileEditSelected in events)
    }

    @Test
    fun logoutAsksForConfirmationThenSendsLogoutSelected() {
        setScreen(signedIn())

        composeRule.onNodeWithContentDescription(string(R.string.account_logout)).performClick()
        composeRule.onNodeWithText(string(R.string.account_logout_confirm_title)).assertIsDisplayed()
        assertTrue(AccountState.Event.LogoutSelected !in events)

        composeRule.onNode(hasText(string(R.string.account_logout)) and hasClickAction()).performClick()

        assertTrue(AccountState.Event.LogoutSelected in events)
    }
}
