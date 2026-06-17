package su.afk.yummy.tv.domain.account.model

data class AccountSession(
    val isAuthorized: Boolean,
    val userId: Int,
)
