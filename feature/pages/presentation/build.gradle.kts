plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.pages.presentation" }

dependencies {
    implementation(project(":core:utils"))
    api(project(":feature:pages:domain"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(libs.bundles.compose.presentation)
}
