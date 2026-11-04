package su.afk.yummy.tv.feature.glossary.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.glossary.IGlossaryNavigator
import javax.inject.Inject

class GlossaryNavigator @Inject constructor() : IGlossaryNavigator {
    override fun getGlossaryDest(): NavKey = GlossaryDestination
}
