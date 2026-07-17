package su.afk.yummy.tv.domain.pages.repository

import su.afk.yummy.tv.domain.pages.model.SitePage
import su.afk.yummy.tv.domain.pages.model.SitePageType

interface SitePagesRepository {
    suspend fun getPage(type: SitePageType): SitePage
}
