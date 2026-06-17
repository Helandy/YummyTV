plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.library"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:library:domain"))
    implementation(project(":feature:library:presentation"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.coil.full)

    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.core)
}
