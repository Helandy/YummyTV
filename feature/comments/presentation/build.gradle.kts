plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.comments.presentation"
}

dependencies {
    implementation(project(":feature:comments:api"))
    api(project(":feature:comments:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)
}
