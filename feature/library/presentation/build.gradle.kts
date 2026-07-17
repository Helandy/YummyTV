plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.library.presentation"
}

dependencies {

    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:library:domain"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:watching:presentation"))

    implementation(libs.bundles.compose.presentation)
}
