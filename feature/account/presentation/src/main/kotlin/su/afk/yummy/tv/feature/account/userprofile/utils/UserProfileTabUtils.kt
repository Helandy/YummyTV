package su.afk.yummy.tv.feature.account.userprofile.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

/** Сколько элементов во вкладке по счётчикам профиля; null для обзора или пока профиль не загружен. */
fun UserProfileState.Tab.contentCount(profile: UserProfileSummary?): Int? {
    if (profile == null) return null
    return when (this) {
        UserProfileState.Tab.OVERVIEW -> null
        UserProfileState.Tab.LISTS -> with(profile.counts) {
            watching + planned + completed + dropped + postponed + favorite
        }

        UserProfileState.Tab.COLLECTIONS -> profile.socialCounts.collections
        UserProfileState.Tab.POSTS -> profile.socialCounts.posts
        UserProfileState.Tab.REVIEWS -> profile.socialCounts.reviews
        UserProfileState.Tab.FRIENDS -> profile.socialCounts.friends
    }
}

/** Обзор и вкладки, в которых есть что показать: пустые (счётчик 0) не нужны. */
internal fun UserProfileSummary.visibleProfileTabs(): ImmutableList<UserProfileState.Tab> =
    UserProfileState.Tab.entries
        .filter { tab -> tab.contentCount(this)?.let { it > 0 } ?: true }
        .toImmutableList()
