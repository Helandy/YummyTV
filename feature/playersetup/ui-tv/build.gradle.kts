plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.playersetup.tv"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:playersetup:api"))
    implementation(project(":feature:playersetup:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
}
