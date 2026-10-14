package su.afk.yummy.tv.feature.account.mobile.localauth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.localauth.LocalAuthState
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class LocalAuthMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<LocalAuthState.Event>()

    private val device = DiscoveredDevice(id = "tv-1", name = "Гостиная ТВ", host = "192.168.1.20", port = 8080)

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: LocalAuthState.State = LocalAuthState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                LocalAuthMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun withDevice(block: LocalAuthState.State.() -> LocalAuthState.State = { this }) =
        LocalAuthState.State(
            devices = persistentListOf(device),
            selectedDevice = device,
        ).block()

    @Test
    fun screenOpenedIsSentOnOpen() {
        setScreen()
        composeRule.waitForIdle()

        assertTrue(LocalAuthState.Event.ScreenOpened in events)
    }

    @Test
    fun searchingShowsProgressText() {
        setScreen(LocalAuthState.State(isSearching = true))

        composeRule.onNodeWithText(string(R.string.account_local_auth_mobile_searching)).assertIsDisplayed()
    }

    @Test
    fun emptyDiscoveryShowsHint() {
        setScreen(LocalAuthState.State(isSearching = false))

        composeRule.onNodeWithText(string(R.string.account_local_auth_discovery_empty)).assertIsDisplayed()
    }

    @Test
    fun foundDeviceIsShownAndClickSendsDeviceSelected() {
        setScreen(LocalAuthState.State(devices = persistentListOf(device)))

        composeRule.onNodeWithText("Гостиная ТВ").assertIsDisplayed()
        composeRule.onNodeWithText("Гостиная ТВ").performClick()

        assertTrue(LocalAuthState.Event.DeviceSelected(device) in events)
    }

    @Test
    fun selectedDeviceAsksForCode() {
        setScreen(withDevice())

        composeRule.onNodeWithText(string(R.string.account_local_auth_enter_pin)).assertIsDisplayed()
    }

    @Test
    fun typingCodeSendsPinChanged() {
        setScreen(withDevice())

        composeRule.onNode(hasSetTextAction()).performTextInput("123")

        assertTrue(events.any { it is LocalAuthState.Event.PinChanged })
    }

    @Test
    fun transferIsDisabledUntilCodeIsComplete() {
        setScreen(withDevice { copy(pin = "123") })

        composeRule
            .onNode(hasText(string(R.string.account_local_auth_transfer_button)) and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun completeCodeEnablesTransferAndSendsTransferSelected() {
        setScreen(withDevice { copy(pin = "A".repeat(LocalAuthState.PIN_LENGTH)) })

        val button = composeRule
            .onNode(hasText(string(R.string.account_local_auth_transfer_button)) and hasClickAction())
        button.assertIsEnabled()
        button.performClick()

        assertTrue(LocalAuthState.Event.TransferSelected in events)
    }

    @Test
    fun errorMessageIsShown() {
        setScreen(withDevice { copy(error = AccountUiError.LOCAL_AUTH_INVALID_PIN) })

        composeRule.onNodeWithText(string(R.string.account_local_auth_error_invalid_pin)).assertIsDisplayed()
    }

    @Test
    fun transferredShowsResultAndDoneSendsBackSelected() {
        setScreen(LocalAuthState.State(isTransferred = true))

        composeRule.onNodeWithText(string(R.string.account_local_auth_mobile_transferred_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_done)).performClick()

        assertTrue(LocalAuthState.Event.BackSelected in events)
    }

    @Test
    fun searchAgainSendsRetrySearchSelected() {
        setScreen()

        composeRule
            .onNode(hasText(string(R.string.account_local_auth_mobile_search_again)) and hasClickAction())
            .performClick()

        assertTrue(LocalAuthState.Event.RetrySearchSelected in events)
    }
}
