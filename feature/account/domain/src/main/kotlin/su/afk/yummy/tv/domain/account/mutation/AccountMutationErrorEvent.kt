package su.afk.yummy.tv.domain.account.mutation

data class AccountMutationErrorEvent(
    val action: AccountMutationAction,
    val message: String?,
)
