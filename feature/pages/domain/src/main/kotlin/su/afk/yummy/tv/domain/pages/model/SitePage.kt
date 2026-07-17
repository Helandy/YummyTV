package su.afk.yummy.tv.domain.pages.model

enum class SitePageType(val apiValue: String) {
    FAQ("faq"),
    ABOUT_SITE("about-site"),
    RULES("rules"),
    REVIEW_RULES("review-rules"),
    COLLECTIONS_RULES("collections-rules"),
    POST_RULES("post-rules"),
    EDIT_RULES("edit-rules"),
    PRIVACY("privacy"),
    COPYRIGHT("copyright"),
    WANT_TO_HELP("want-to-help"),
}

data class SitePage(
    val title: String,
    val text: String,
)
