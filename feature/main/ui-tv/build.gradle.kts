plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.main"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:update"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:library:ui-tv"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":feature:schedule:ui-tv"))
    implementation(project(":feature:search:ui-tv"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:top:ui-tv"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
