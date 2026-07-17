plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.posts.mobile" }

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:posts:api"))
    implementation(project(":feature:posts:domain"))
    implementation(project(":feature:posts:presentation"))
    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
}
