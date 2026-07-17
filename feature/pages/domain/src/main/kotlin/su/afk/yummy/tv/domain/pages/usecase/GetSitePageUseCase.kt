package su.afk.yummy.tv.domain.pages.usecase

import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.repository.SitePagesRepository
import javax.inject.Inject

class GetSitePageUseCase @Inject constructor(
    private val repository: SitePagesRepository,
) {
    suspend operator fun invoke(type: SitePageType) = repository.getPage(type)
}
