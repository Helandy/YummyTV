package su.afk.yummy.tv.feature.schedule.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import javax.inject.Inject

/**
 * Разбирает `yummytv://schedule` в расписание.
 *
 * На ТВ расписание — корневая вкладка меню, поэтому переключаем корень (иначе экран открылся бы
 * поверх Главной без левой панели) и ключ не возвращаем. На мобилке вкладки расписания нет, там
 * это отдельный экран поверх стека.
 */
class ScheduleDeepLinkResolver @Inject constructor(
    private val navManager: INavigationManager,
    private val scheduleNavigator: IScheduleNavigator,
    private val interfaceModePreferences: AppInterfaceModePreferences,
) : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        if (link.appLinkHost != "schedule") return null
        if (interfaceModePreferences.selectedMode == AppInterfaceMode.TV) {
            navManager.switchRoot(RootTab.SCHEDULE, reselectPopToRoot = false)
            return null
        }
        return scheduleNavigator.getScheduleDest()
    }
}
