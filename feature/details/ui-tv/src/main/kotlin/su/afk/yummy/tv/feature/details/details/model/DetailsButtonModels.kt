package su.afk.yummy.tv.feature.details.details.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction

internal enum class ButtonStyle { Filled, Outlined, Normal }

internal data class ButtonData(
    val action: DetailsButtonAction,
    val label: String,
    val icon: ImageVector,
    val style: ButtonStyle,
    val onClick: () -> Unit,
    val overrideColor: Color? = null,
    val enabled: Boolean = true,
)

internal sealed interface ButtonRowData {
    val key: String
    val buttonIndices: List<Int>

    data class Single(val index: Int, val button: ButtonData) : ButtonRowData {
        override val key = button.action.name
        override val buttonIndices = listOf(index)
    }

    data class Pair(
        val firstIndex: Int,
        val first: ButtonData,
        val secondIndex: Int?,
        val second: ButtonData?,
    ) : ButtonRowData {
        override val key = "${first.action.name}_${second?.action?.name.orEmpty()}"
        override val buttonIndices = listOfNotNull(firstIndex, secondIndex)
    }
}
