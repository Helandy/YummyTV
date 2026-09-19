package su.afk.yummy.tv.core.designsystem.mobile.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Полноэкранный лоадер, блокирующий ввод: показывается, пока идёт запрос перед переходом на
 * другой экран. Диалог — чтобы перекрыть контент `BaseScreen` (его content — ColumnScope) и
 * заодно съесть тапы и системный «назад».
 */
@Composable
fun MobileBlockingLoading(modifier: Modifier = Modifier) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
