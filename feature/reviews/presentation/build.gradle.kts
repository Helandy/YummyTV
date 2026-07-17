plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.reviews.presentation" }

dependencies {
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:details:api"))
    api(project(":feature:reviews:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:reviews:api"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)
    implementation(libs.jsoup)
}
