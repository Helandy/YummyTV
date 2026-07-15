plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.player.common"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:player:presentation"))
    implementation(project(":feature:video-download:data"))
    implementation(libs.bundles.media3.player)
    implementation(libs.bundles.compose.screen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
