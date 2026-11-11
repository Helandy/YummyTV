package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMenuActions
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMenuModel
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Страница «Качество»: переключатель адаптивного режима и список качеств. При адаптации список
 * выбирает потолок качества (плюс «Лучшее» — без потолка), иначе — конкретное качество.
 */
@Composable
internal fun MobilePlayerQualityPage(
    model: MobilePlayerSettingsMenuModel,
    actions: MobilePlayerSettingsMenuActions,
    onBack: () -> Unit,
) {
    val bestLabel = stringResource(R.string.player_quality_best)
    Column(modifier = Modifier.fillMaxWidth()) {
        MobilePlayerMenuPageHeader(title = stringResource(R.string.player_quality_title), onBack = onBack)
        if (model.qualities.size > 1) {
            MobilePlayerMenuRow(
                label = stringResource(R.string.player_adaptive_quality_title),
                trailing = MobilePlayerMenuRowTrailing.Toggle(model.adaptiveQuality),
                onClick = { actions.onAdaptiveQualityChange(!model.adaptiveQuality) },
            )
            Text(
                text = if (model.adaptiveQuality) {
                    stringResource(
                        R.string.player_adaptive_quality_current_limit,
                        model.selectedQuality.orEmpty(),
                        if (model.adaptiveLimitBest) bestLabel else model.adaptiveLimitQuality.orEmpty(),
                    )
                } else {
                    stringResource(R.string.player_adaptive_quality_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 52.dp, end = 8.dp, bottom = 8.dp),
            )
        }
        if (model.adaptiveQuality) {
            MobilePlayerMenuRow(
                label = bestLabel,
                trailing = MobilePlayerMenuRowTrailing.Choice(model.adaptiveLimitBest),
                onClick = {
                    actions.onAdaptiveLimitBestSelected()
                    onBack()
                },
            )
        }
        // От лучшего к худшему, как у YouTube: список качеств приходит по возрастанию.
        model.qualities.asReversed().forEach { quality ->
            val selected = if (model.adaptiveQuality) {
                !model.adaptiveLimitBest && quality == model.adaptiveLimitQuality
            } else {
                quality == model.selectedQuality
            }
            MobilePlayerMenuRow(
                label = quality,
                trailing = MobilePlayerMenuRowTrailing.Choice(selected),
                onClick = {
                    if (model.adaptiveQuality) {
                        actions.onAdaptiveLimitSelected(quality)
                    } else {
                        actions.onQualitySelected(quality)
                    }
                    onBack()
                },
            )
        }
    }
}
