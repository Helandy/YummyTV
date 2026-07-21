package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Загружает справочник доступных блогеров и категорий. */
class GetBloggersDirectoryUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke() = repository.getDirectory()
}
