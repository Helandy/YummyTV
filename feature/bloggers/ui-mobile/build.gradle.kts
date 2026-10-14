plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.bloggers.mobile"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:bloggers:presentation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(libs.androidx.paging.compose)
    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.coil.full)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.compose.uiTest.manifest)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTest.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
}
