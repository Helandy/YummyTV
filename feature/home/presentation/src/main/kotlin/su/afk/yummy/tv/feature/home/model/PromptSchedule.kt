package su.afk.yummy.tv.feature.home.model

/** Где диалог находится по сроку показа. */
internal enum class PromptSchedule {
    /** Уже закрыт или показан — больше не предлагаем. */
    Dismissed,

    /** Срок ещё не наступил; следом придёт [Due]. */
    Waiting,

    /** Срок наступил, диалог можно показывать. */
    Due,
}
