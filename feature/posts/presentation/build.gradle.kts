plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.posts.presentation" }

dependencies {
    implementation(project(":feature:comments:api"))
    api(project(":feature:posts:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:posts:api"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)
    implementation(libs.jsoup)
}
