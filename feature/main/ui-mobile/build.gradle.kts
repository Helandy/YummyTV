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
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:commonScreen:api"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:faq:api"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:library:ui-mobile"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":feature:messages:api"))
    implementation(project(":feature:pages:api"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:posts:api"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":feature:schedule:ui-mobile"))
    implementation(project(":feature:search:api"))
    implementation(project(":feature:search:ui-mobile"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:top:api"))
    implementation(project(":feature:top:ui-mobile"))
    implementation(project(":feature:update:api"))
    implementation(project(":feature:video-download:api"))
    implementation(project(":feature:watch-later:api"))

    implementation(libs.bundles.compose.core)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
