package su.afk.yummy.tv.android.search

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import su.afk.yummy.tv.R
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.usecase.SearchUseCase
import kotlin.time.Duration.Companion.milliseconds

class SystemSearchProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val appContext = appContext()
        if (uri.authority != appContext.getString(R.string.search_suggest_authority)) {
            return emptyCursor()
        }

        val query = selectionArgs?.firstOrNull()
            ?: uri.pathSegments.lastOrNull()
            ?: ""
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return emptyCursor()

        return runCatching {
            val items = runBlocking {
                withTimeout(SEARCH_TIMEOUT) {
                    searchUseCase(appContext)(
                        normalizedQuery,
                        SearchFilters.EMPTY,
                        SEARCH_LIMIT,
                        0,
                    ).items
                }
            }
            items.toCursor(appContext)
        }.getOrElse {
            emptyCursor()
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun List<SearchItem>.toCursor(context: Context): Cursor =
        MatrixCursor(SUGGESTION_COLUMNS).apply {
            forEach { item ->
                addRow(
                    arrayOf<Any?>(
                        item.id,
                        item.title,
                        item.rating?.let { context.getString(R.string.search_suggest_rating, it) }
                            ?: context.getString(R.string.search_suggest_anime),
                        item.posterUrl,
                        CONTENT_TYPE_VIDEO,
                        null,
                        null,
                        item.id.toString(),
                    )
                )
            }
        }

    private fun emptyCursor(): Cursor = MatrixCursor(SUGGESTION_COLUMNS)

    private fun searchUseCase(context: Context): SearchUseCase =
        EntryPointAccessors.fromApplication(
            context,
            SystemSearchProviderEntryPoint::class.java,
        ).searchUseCase()

    private fun appContext(): Context =
        checkNotNull(context?.applicationContext) { "ContentProvider is not attached." }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SystemSearchProviderEntryPoint {
        fun searchUseCase(): SearchUseCase
    }

    private companion object {
        const val SEARCH_LIMIT = 10
        val SEARCH_TIMEOUT = 2_500.milliseconds
        const val MIN_QUERY_LENGTH = 2
        const val CONTENT_TYPE_VIDEO = "video/*"

        val SUGGESTION_COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
            SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
            SearchManager.SUGGEST_COLUMN_DURATION,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
        )
    }
}
