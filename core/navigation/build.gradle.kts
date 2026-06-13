plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.navigation"
}

dependencies {
    implementation(libs.bundles.compose.core)

    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.androidx.navigation3.viewmodel)
}
