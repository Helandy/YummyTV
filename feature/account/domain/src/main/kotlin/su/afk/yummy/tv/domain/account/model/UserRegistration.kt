package su.afk.yummy.tv.domain.account.model

data class UserRegistration(
    val email: String,
    val password: String,
    val username: String,
    val captchaResponse: String? = null,
    val hash: String? = null,
    val shikimori: String? = null,
    val vk: String? = null,
)
