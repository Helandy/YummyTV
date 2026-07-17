plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.home.mobile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:bloggers:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:home:presentation"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.bundles.coil.full)
}
