package su.afk.yummy.tv.domain.account.model

data class AccountMutationErrorEvent(
    val action: AccountMutationAction,
    val message: String?,
)
