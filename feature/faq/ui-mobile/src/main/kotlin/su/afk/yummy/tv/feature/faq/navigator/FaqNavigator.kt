package su.afk.yummy.tv.feature.faq.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import javax.inject.Inject

class FaqNavigator @Inject constructor() : IFaqNavigator {
    override fun getFaqDest(): NavKey = FaqDestination
}
