package su.afk.yummy.tv.domain.account

interface AccountRepository {
    suspend fun login(login: String, password: String): YaniAccount
    suspend fun refreshToken(): YaniAccount?
    suspend fun getProfile(): YaniAccount
    suspend fun logout()
}
