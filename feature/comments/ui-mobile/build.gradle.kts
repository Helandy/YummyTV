plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.comments.mobile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:comments:domain"))
    implementation(project(":feature:comments:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
}
