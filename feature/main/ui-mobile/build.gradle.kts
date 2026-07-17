plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.main.mobile"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:update"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:faq:api"))
    implementation(project(":feature:pages:api"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:library:ui-mobile"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":feature:schedule:ui-mobile"))
    implementation(project(":feature:search:api"))
    implementation(project(":feature:search:ui-mobile"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:top:ui-mobile"))

    implementation(libs.bundles.compose.core)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
