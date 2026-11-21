plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.details"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:comments:presentation"))
    implementation(project(":feature:comments:ui-tv"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:presentation"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:reviews:presentation"))
    implementation(project(":feature:reviews:ui-tv"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.coil.full)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
