package su.afk.yummy.tv.feature.details.relation.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.feature.details.full.view.FullDetailsChip
import su.afk.yummy.tv.feature.details.relation.model.RelationType
import su.afk.yummy.tv.feature.details.relation.utils.labelRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RelationTvHeaderCard(
    relationType: RelationType,
    relation: AnimeRelation,
    onSubGenreSelected: (Int) -> Unit,
    subGenreFocusRequesters: Map<Int, FocusRequester> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val hasSubGenres = relation.subGenres.isNotEmpty()
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .then(if (hasSubGenres) Modifier.focusGroup() else Modifier),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Фокус-стоп — отдельный от чипов элемент (сиблинг, а не их родитель):
            // если сделать фокусируемой всю карточку целиком, вложенные фокусируемые
            // чипы окажутся внутри её же границ, и directional focus search пультом
            // не находит их как цель "ниже" — навигация на чипы перестаёт работать.
            Column(
                // Без tvFocusIndicator — фокус-стоп нужен только для навигации пультом,
                // цветная рамка на самом описании не нужна.
                modifier = modifier.focusable(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(relationType.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = relation.title,
                    style = MaterialTheme.typography.displaySmall,
                )
                relation.secondaryTitle?.let { secondaryTitle ->
                    Text(
                        text = secondaryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                relation.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasSubGenres) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    relation.subGenres.forEach { genre ->
                        FullDetailsChip(
                            label = genre.title,
                            onClick = { onSubGenreSelected(genre.id) },
                            modifier = subGenreFocusRequesters[genre.id]
                                ?.let { Modifier.focusRequester(it) }
                                ?: Modifier,
                        )
                    }
                }
            }
        }
    }
}
