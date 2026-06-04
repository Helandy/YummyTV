package su.afk.yummy.tv.feature.details.details.model

import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction

internal enum class ButtonStyle { Filled, Outlined, Normal }

internal data class ButtonData(
    val action: DetailsButtonAction,
    val label: String,
    val icon: ImageVector,
    val style: ButtonStyle,
    val onClick: () -> Unit,
)

internal sealed interface ButtonRowData {
    val key: String
    val buttonIndices: List<Int>

    data class Single(val index: Int, val button: ButtonData) : ButtonRowData {
        override val key = button.action.name
        override val buttonIndices = listOf(index)
    }

    data class LibraryFavorite(
        val libraryIndex: Int,
        val libraryButton: ButtonData,
        val favoriteIndex: Int,
        val favoriteButton: ButtonData,
    ) : ButtonRowData {
        override val key = "${libraryButton.action.name}_${favoriteButton.action.name}"
        override val buttonIndices = listOf(libraryIndex, favoriteIndex)
    }
}
