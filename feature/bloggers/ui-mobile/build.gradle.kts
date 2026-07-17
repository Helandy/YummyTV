plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.bloggers.mobile" }

dependencies {
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:bloggers:presentation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.coil.full)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
