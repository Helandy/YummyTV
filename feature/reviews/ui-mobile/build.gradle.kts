plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.reviews.mobile" }

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:reviews:domain"))
    implementation(project(":feature:reviews:presentation"))
    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
}
