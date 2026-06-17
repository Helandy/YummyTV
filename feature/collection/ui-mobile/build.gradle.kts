plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.collection.mobile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:collection:domain"))
    implementation(project(":feature:collection:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.coil.compose)
}
