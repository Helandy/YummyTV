plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.collection"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:collection:presentation"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.coil.full)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
