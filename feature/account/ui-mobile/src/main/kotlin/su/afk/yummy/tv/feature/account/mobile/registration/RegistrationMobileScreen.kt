package su.afk.yummy.tv.feature.account.mobile.registration

import android.widget.Toast
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.feature.account.mobile.view.RegistrationForm
import su.afk.yummy.tv.feature.account.mobile.view.RegistrationSuccessView
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.registration.RegistrationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationMobileScreen(
    state: RegistrationState.State,
    effect: Flow<RegistrationState.Effect>,
    onEvent: (RegistrationState.Event) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val autofillManager = LocalAutofillManager.current
    val context = LocalContext.current
    val captchaHint = stringResource(R.string.account_captcha_required_toast)

    effect.collectAsEffect { effectItem ->
        when (effectItem) {
            RegistrationState.Effect.HideKeyboard -> {
                keyboardController?.hide()
                focusManager.clearFocus()
            }

            RegistrationState.Effect.ShowCaptchaHint -> Toast.makeText(context, captchaHint, Toast.LENGTH_SHORT).show()
            RegistrationState.Effect.DiscardAutofill -> autofillManager?.cancel()
        }
    }

    BaseScreen(
        isScroll = false,
        contentModifier = Modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_registration_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(RegistrationState.Event.BackSelected) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.common_back),
                        )
                    }
                },
            )
        },
    ) {
        if (state.isSuccess) {
            RegistrationSuccessView(onBack = { onEvent(RegistrationState.Event.BackSelected) })
        } else {
            RegistrationForm(state, onEvent)
        }
    }
}
