package su.afk.yummy.tv.feature.faq.mobile.model

object FaqState {
    data object State

    sealed interface Event {
        data object BackSelected : Event
    }

    sealed interface Effect
}
