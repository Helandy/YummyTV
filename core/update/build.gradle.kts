plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.update"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.androidx.core.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.androidx.activity.compose)
}
