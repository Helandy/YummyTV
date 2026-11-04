plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.glossary.mobile"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:glossary:api"))
    implementation(project(":feature:glossary:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.compose.uiTest.manifest)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTest.junit4)
    // Compose тянет espresso 3.5.0, он падает на API 36+ (InputManager.getInstance убран).
    androidTestImplementation(libs.androidx.espresso.core)
}
