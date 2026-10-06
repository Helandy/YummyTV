package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

/**
 * Третья колонка: списки, по которым блок новых серий отбирает тайтлы. Выбор множественный,
 * поэтому OK переключает список и колонку не закрывает — в отличие от обычных пикеров.
 */
@Composable
internal fun NewEpisodesSourcesPanel(
    selected: Set<NewEpisodesSource>,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onToggle: (NewEpisodesSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NewEpisodesSource.entries.forEachIndexed { index, source ->
            key(source) {
                ToggleRow(
                    label = source.label(),
                    hint = if (source in selected) {
                        stringResource(R.string.settings_new_episodes_source_on)
                    } else {
                        stringResource(R.string.settings_new_episodes_source_off)
                    },
                    enabled = source in selected,
                    onClick = { onToggle(source) },
                    modifier = Modifier
                        .then(
                            if (index == 0 && contentFocusRequester != null) {
                                Modifier.focusRequester(contentFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .restoreCategoryFocusOnLeft(upFocusRequester),
                )
            }
        }
    }
}
