plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.reviews.mobile"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:reviews:domain"))
    implementation(project(":feature:reviews:presentation"))
    implementation(libs.bundles.compose.screen)
    implementation(libs.compose.material3.adaptive.navigation3)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)

    debugImplementation(libs.compose.uiTest.manifest)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTest.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
}
