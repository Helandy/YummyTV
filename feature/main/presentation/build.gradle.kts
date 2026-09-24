plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.main.presentation"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    implementation(project(":core:featuretoggle"))
    implementation(project(":core:model"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:update:domain"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.presentation)
}
