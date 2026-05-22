package su.afk.yummy.tv.core.designsystem.presenter.baseViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> ScreenNavigator(
    viewModel: BaseViewModelNew<S, E, F>,
    content: @Composable (state: S, effect: Flow<F>, onEventSent: (E) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    content(state, viewModel.effect, viewModel::setEvent)
}

