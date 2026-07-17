plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.pages.mobile" }

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:pages:api"))
    implementation(project(":feature:pages:domain"))
    implementation(project(":feature:pages:presentation"))
    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.material.icons.extended)
}
