package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun GestureTutorialIllustration(
    step: GestureTutorialStep,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (step) {
            GestureTutorialStep.Tap -> TapTutorialIllustration(accent)
            GestureTutorialStep.Seek -> SeekTutorialIllustration(accent)
            GestureTutorialStep.Vertical -> VerticalTutorialIllustration(accent)
            GestureTutorialStep.Zoom -> ZoomTutorialIllustration(accent)
            GestureTutorialStep.Hold -> HoldTutorialIllustration(accent)
        }
    }
}
