package su.afk.yummy.tv.feature.account.mobile.profileedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class ProfileEditMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ProfileEditState.Event>()

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    // нижние секции ещё не скомпонованы в LazyColumn, поэтому сначала прокручиваем список к нужному тексту
    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun loaded(block: ProfileEditState.State.() -> ProfileEditState.State = { this }) =
        ProfileEditState.State(isLoading = false, nickname = "ТестовыйНик").block()

    private fun setScreen(state: ProfileEditState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ProfileEditMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndSections() {
        setScreen(loaded())

        composeRule.onNodeWithText(string(R.string.profile_edit_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.profile_edit_main)).assertIsDisplayed()
    }

    @Test
    fun loadingHidesSaveButton() {
        setScreen(ProfileEditState.State(isLoading = true))

        composeRule.onNodeWithText(string(R.string.profile_edit_save)).assertDoesNotExist()
    }

    @Test
    fun loadErrorRetrySendsRetrySelected() {
        setScreen(ProfileEditState.State(isLoading = false, hasLoadError = true))

        composeRule.onNodeWithText(string(R.string.profile_edit_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(ProfileEditState.Event.RetrySelected in events)
    }

    @Test
    fun typingAboutSendsAboutChanged() {
        setScreen(loaded())

        composeRule
            .onNode(hasSetTextAction() and hasText(string(R.string.profile_edit_about)))
            .performScrollTo()
            .performTextInput("Люблю аниме")

        assertTrue(ProfileEditState.Event.AboutChanged("Люблю аниме") in events)
    }

    @Test
    fun saveButtonSendsSaveSelected() {
        setScreen(loaded())

        scrollToText(string(R.string.profile_edit_save))
        composeRule.onNodeWithText(string(R.string.profile_edit_save)).performClick()

        assertTrue(ProfileEditState.Event.SaveSelected in events)
    }

    @Test
    fun changePasswordButtonSendsChangePasswordSelected() {
        setScreen(loaded())

        scrollToText(string(R.string.profile_change_password))
        composeRule
            .onNode(hasText(string(R.string.profile_change_password)) and hasClickAction())
            .performClick()

        assertTrue(ProfileEditState.Event.ChangePasswordSelected in events)
    }

    @Test
    fun passwordValidationErrorIsShown() {
        setScreen(loaded { copy(passwordValidationError = true) })

        scrollToText(string(R.string.profile_password_validation_error))
        composeRule.onNodeWithText(string(R.string.profile_password_validation_error)).assertIsDisplayed()
    }

    @Test
    fun noLinkedAccountsShowsMessage() {
        setScreen(loaded())

        scrollToText(string(R.string.profile_linked_accounts_empty))
        composeRule.onNodeWithText(string(R.string.profile_linked_accounts_empty)).assertIsDisplayed()
    }

    @Test
    fun unlinkDialogConfirmSendsUnlinkAccountConfirmed() {
        setScreen(
            loaded {
                copy(
                    linkedAccounts = persistentSetOf(LinkedAccountProvider.VK),
                    pendingUnlinkAccount = LinkedAccountProvider.VK,
                )
            },
        )

        composeRule
            .onNode(hasText(string(R.string.profile_unlink_account)) and hasClickAction())
            .performClick()

        assertTrue(ProfileEditState.Event.UnlinkAccountConfirmed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(loaded())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(ProfileEditState.Event.BackSelected in events)
    }
}
