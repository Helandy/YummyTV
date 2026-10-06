package su.afk.yummy.tv.feature.account.passwordreset

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.account.passwordreset.handler.PasswordResetHandler

/**
 * Отправка формы проверяет почту через `android.util.Patterns`, которого в JVM-тестах нет,
 * поэтому здесь покрыты только события, не доходящие до валидации.
 */
class PasswordResetViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val handler: PasswordResetHandler = mockk()

    private fun createViewModel() = PasswordResetViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        handler = handler,
    )

    @Test
    fun `starts with an empty form`() {
        val state = createViewModel().currentState

        assertEquals("", state.email)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `typing the email clears errors and refreshes the captcha challenge`() {
        val vm = createViewModel()
        vm.setEvent(PasswordResetState.Event.CaptchaFailed)
        val challenge = vm.currentState.captchaChallengeId

        vm.setEvent(PasswordResetState.Event.EmailChanged("a@b.c"))

        val state = vm.currentState
        assertEquals("a@b.c", state.email)
        assertFalse(state.validationError)
        assertFalse(state.requestError)
        assertFalse(state.isCaptchaRequired)
        assertEquals(challenge + 1, state.captchaChallengeId)
    }

    @Test
    fun `blank captcha token is a captcha error`() {
        val vm = createViewModel()

        vm.setEvent(PasswordResetState.Event.CaptchaSolved(" "))

        assertTrue(vm.currentState.captchaError)
    }

    @Test
    fun `expired captcha is a captcha error with a new challenge`() {
        val vm = createViewModel()

        vm.setEvent(PasswordResetState.Event.CaptchaExpired)

        assertTrue(vm.currentState.captchaError)
        assertEquals(1, vm.currentState.captchaChallengeId)
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(PasswordResetState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
