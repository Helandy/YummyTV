plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.search.mobile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:search:api"))
    implementation(project(":feature:search:domain"))
    implementation(project(":feature:search:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.coil.compose)
}
