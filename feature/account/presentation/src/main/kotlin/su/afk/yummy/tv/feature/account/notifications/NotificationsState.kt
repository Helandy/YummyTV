package su.afk.yummy.tv.feature.account.notifications

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

/** Состояние списка уведомлений аккаунта; общее для мобильного экрана и вкладки на ТВ. */
class NotificationsState {
    @Immutable
    data class State(
        val notifications: ImmutableList<ProfileNotification> = persistentListOf(),
        val notificationCounts: ImmutableList<NotificationCount> = persistentListOf(),
        /** Идёт определение аниме по ссылке уведомления перед переходом. */
        val isNotificationOpening: Boolean = false,
        /** Уведомления, развёрнутые пользователем до полного текста. */
        val expandedNotificationIds: PersistentSet<Int> = persistentSetOf(),
        val isNotificationsLoading: Boolean = false,
        val isNotificationsLoadingMore: Boolean = false,
        /** Последняя страница оказалась полной — на сервере могут быть ещё уведомления. */
        val hasMoreNotifications: Boolean = false,
        val error: AccountUiError? = null,
        /** Техническая причина ошибки загрузки (код ответа, исключение). */
        val errorDetails: String? = null,
    ) : UiState {
        val unreadNotificationCounts: ImmutableList<NotificationCount>
            get() = notificationCounts.filterNot { it.type.equals("message", ignoreCase = true) }
                .toImmutableList()

        val unreadNotificationCount: Int
            get() = unreadNotificationCounts.sumOf { it.count }
    }

    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Экран показан — перечитываем уведомления, если их кэш успел устареть. */
        data object ScreenShown : Event

        /** Пользователь запросил повторную загрузку после ошибки. */
        data object RefreshSelected : Event

        /** Переход к списку подписок пользователя. */
        data object MySubscriptionsSelected : Event

        /** Пользователь открыл уведомление с указанным идентификатором. */
        data class NotificationSelected(val id: Int) : Event

        /** Пользователь развернул или свернул уведомление с указанным идентификатором. */
        data class NotificationExpandToggled(val id: Int) : Event

        /** Пользователь выбрал ссылку на аниме [slug] в тексте уведомления [notificationId]. */
        data class NotificationLinkSelected(val notificationId: Int, val slug: String) : Event

        /** Пользователь отметил уведомление с указанным идентификатором прочитанным. */
        data class NotificationReadSelected(val id: Int) : Event

        /** Пользователь отметил все уведомления прочитанными. */
        data object AllNotificationsReadSelected : Event

        /** Пользователь удалил все уведомления. */
        data object AllNotificationsDeleteSelected : Event

        /** Пользователь запросил следующую страницу уведомлений. */
        data object NotificationsLoadMoreSelected : Event

        /** Пользователь удалил уведомление с указанным идентификатором. */
        data class NotificationDeleteSelected(val id: Int) : Event
    }

    sealed interface Effect : UiEffect
}
