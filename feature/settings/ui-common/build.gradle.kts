plugins {
    id("yummytv.android.library.compose")
}

android {
    namespace = "su.afk.yummy.tv.feature.settings.common"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:preferences"))
    implementation(project(":feature:settings:presentation"))

    implementation(libs.bundles.compose.core)
    implementation(libs.androidx.material.icons.extended)
}
