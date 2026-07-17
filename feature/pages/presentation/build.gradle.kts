plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.pages.presentation" }

dependencies {
    api(project(":feature:pages:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(libs.bundles.compose.presentation)
}
