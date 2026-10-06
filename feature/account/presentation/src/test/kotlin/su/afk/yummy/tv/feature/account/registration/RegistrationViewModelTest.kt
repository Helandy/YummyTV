package su.afk.yummy.tv.feature.account.registration

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.RegistrationException
import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.RegisterUserUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class RegistrationViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val repository: AccountRepository = mockk(relaxed = true)

    private fun createViewModel() = RegistrationViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        registerUserUseCase = RegisterUserUseCase(repository),
    )

    private fun RegistrationViewModel.fill(
        email: String = "user@example.com",
        username: String = "user",
        password: String = "secret123",
    ) {
        setEvent(RegistrationState.Event.EmailChanged(email))
        setEvent(RegistrationState.Event.UsernameChanged(username))
        setEvent(RegistrationState.Event.PasswordChanged(password))
    }

    @Test
    fun `typing a field clears the previous error`() {
        val vm = createViewModel()
        vm.setEvent(RegistrationState.Event.RegisterSelected)
        assertEquals(AccountUiError.CREDENTIALS_REQUIRED, vm.currentState.error)

        vm.setEvent(RegistrationState.Event.EmailChanged("a"))

        assertNull(vm.currentState.error)
        assertEquals("a", vm.currentState.email)
    }

    @Test
    fun `empty form is rejected without a request`() {
        val vm = createViewModel()

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertEquals(AccountUiError.CREDENTIALS_REQUIRED, vm.currentState.error)
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `malformed email is rejected`() {
        val vm = createViewModel()
        vm.fill(email = "not-an-email")

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertEquals(AccountUiError.INVALID_EMAIL, vm.currentState.error)
    }

    @Test
    fun `short password is rejected`() {
        val vm = createViewModel()
        vm.fill(password = "123")

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertEquals(AccountUiError.PASSWORD_TOO_SHORT, vm.currentState.error)
    }

    @Test
    fun `valid form registers the user`() {
        val vm = createViewModel()
        vm.fill()

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertTrue(vm.currentState.isSuccess)
        assertFalse(vm.currentState.isLoading)
        coVerify(exactly = 1) {
            repository.register(UserRegistration(email = "user@example.com", username = "user", password = "secret123", captchaResponse = null))
        }
    }

    @Test
    fun `captcha demand asks for a captcha and shows the hint`() = runTest {
        coEvery { repository.register(any()) } throws AccountCaptchaRequiredException()
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.fill()

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertTrue(vm.currentState.isCaptchaRequired)
        assertNull(vm.currentState.error)
        assertEquals(
            listOf(RegistrationState.Effect.HideKeyboard, RegistrationState.Effect.ShowCaptchaHint),
            effects,
        )
    }

    @Test
    fun `solved captcha repeats the registration with the token`() {
        coEvery { repository.register(match { it.captchaResponse == null }) } throws AccountCaptchaRequiredException()
        val vm = createViewModel()
        vm.fill()
        vm.setEvent(RegistrationState.Event.RegisterSelected)

        vm.setEvent(RegistrationState.Event.CaptchaSolved("token"))

        assertTrue(vm.currentState.isSuccess)
        coVerify(exactly = 1) { repository.register(match { it.captchaResponse == "token" }) }
    }

    @Test
    fun `rejected captcha token reports the rejection`() {
        coEvery { repository.register(any()) } throws AccountCaptchaRequiredException()
        val vm = createViewModel()
        vm.fill()

        vm.setEvent(RegistrationState.Event.CaptchaSolved("token"))

        assertEquals(AccountUiError.CAPTCHA_REJECTED, vm.currentState.error)
        assertTrue(vm.currentState.isCaptchaRequired)
    }

    @Test
    fun `blank captcha token is an error`() {
        val vm = createViewModel()
        vm.fill()

        vm.setEvent(RegistrationState.Event.CaptchaSolved(" "))

        assertEquals(AccountUiError.CAPTCHA_RESPONSE_EMPTY, vm.currentState.error)
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `server refusal keeps its message and discards autofill`() = runTest {
        coEvery { repository.register(any()) } throws RegistrationException("Nick is taken")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.fill()

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertEquals(AccountUiError.REGISTRATION_FAILED, vm.currentState.error)
        assertEquals("Nick is taken", vm.currentState.errorMessage)
        assertEquals(listOf(RegistrationState.Effect.DiscardAutofill), effects)
    }

    @Test
    fun `unexpected failure is a generic registration error`() {
        coEvery { repository.register(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        vm.fill()

        vm.setEvent(RegistrationState.Event.RegisterSelected)

        assertEquals(AccountUiError.REGISTRATION_FAILED, vm.currentState.error)
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `expired captcha stops loading and refreshes the challenge`() {
        val vm = createViewModel()

        vm.setEvent(RegistrationState.Event.CaptchaExpired)

        assertEquals(AccountUiError.CAPTCHA_EXPIRED, vm.currentState.error)
        assertEquals(1, vm.currentState.captchaChallengeId)
    }

    @Test
    fun `failed captcha load is reported`() {
        val vm = createViewModel()

        vm.setEvent(RegistrationState.Event.CaptchaFailed())

        assertEquals(AccountUiError.CAPTCHA_LOAD_FAILED, vm.currentState.error)
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(RegistrationState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
