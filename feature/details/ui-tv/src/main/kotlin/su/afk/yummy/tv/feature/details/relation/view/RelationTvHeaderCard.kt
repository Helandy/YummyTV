package su.afk.yummy.tv.feature.details.relation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.full.view.FullDetailsChip
import su.afk.yummy.tv.feature.details.relation.model.RelationType
import su.afk.yummy.tv.feature.details.relation.utils.labelRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RelationTvHeaderCard(
    relationType: RelationType,
    relation: AnimeRelation,
    onSubGenreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(relationType.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = relation.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
            relation.secondaryTitle?.let { secondaryTitle ->
                Text(
                    text = secondaryTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = stringResource(
                        R.string.details_relation_titles_found,
                        relation.anime.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            relation.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (relation.subGenres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    relation.subGenres.forEach { genre ->
                        FullDetailsChip(
                            label = genre.title,
                            onClick = { onSubGenreSelected(genre.id) },
                        )
                    }
                }
            }
        }
    }
}
