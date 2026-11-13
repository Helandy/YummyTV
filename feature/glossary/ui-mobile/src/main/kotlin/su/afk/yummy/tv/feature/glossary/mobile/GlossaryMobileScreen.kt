package su.afk.yummy.tv.feature.glossary.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.utils.system.COMMUNITY_TELEGRAM_URL
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.glossary.GlossaryState
import su.afk.yummy.tv.feature.glossary.mobile.view.GlossaryCategoryHeader
import su.afk.yummy.tv.feature.glossary.mobile.view.GlossaryEntryCard
import su.afk.yummy.tv.feature.glossary.mobile.view.GlossarySearchField
import su.afk.yummy.tv.feature.glossary.mobile.view.GlossarySuggestButton
import su.afk.yummy.tv.feature.glossary.presentation.R
import su.afk.yummy.tv.feature.glossary.utils.GLOSSARY_ENTRIES
import su.afk.yummy.tv.feature.glossary.utils.filterByQuery
import su.afk.yummy.tv.feature.glossary.utils.groupByCategory
import su.afk.yummy.tv.feature.glossary.utils.toText

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun GlossaryMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        GlossaryMobileScreen(
            state = GlossaryState.State(),
            effect = emptyFlow(),
            onEvent = {},
        )
    }

@Composable
fun GlossaryMobileScreen(
    state: GlossaryState.State,
    effect: Flow<GlossaryState.Effect>,
    onEvent: (GlossaryState.Event) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Тексты зависят от локали, поэтому пересобираем их при смене конфигурации.
    val texts = remember(configuration) { GLOSSARY_ENTRIES.map { it.toText(context.resources) } }
    val groups = remember(texts, state.query) { texts.filterByQuery(state.query).groupByCategory() }
    val isSearching = state.query.isNotBlank()

    effect.collectAsEffect { }

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.glossary_title),
                onBack = { onEvent(GlossaryState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search") {
                GlossarySearchField(
                    query = state.query,
                    onQueryChange = { onEvent(GlossaryState.Event.QueryChanged(it)) },
                )
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.glossary_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            groups.forEach { (category, entries) ->
                // Во время поиска разделы всегда раскрыты, иначе совпадения в свёрнутом разделе не видно.
                val collapsed = !isSearching && category in state.collapsedCategories
                item(key = "category_${category.name}") {
                    GlossaryCategoryHeader(
                        title = stringResource(category.title),
                        count = entries.size,
                        collapsed = collapsed,
                        onClick = { onEvent(GlossaryState.Event.CategoryToggled(category)) },
                    )
                }
                if (!collapsed) {
                    items(entries, key = { "entry_${it.key}" }) { entry ->
                        GlossaryEntryCard(entry = entry)
                    }
                }
            }
            item(key = "suggest") {
                GlossarySuggestButton(onClick = { context.openExternalUri(COMMUNITY_TELEGRAM_URL) })
            }
        }
    }
}
