plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.home.presentation"
}

dependencies {
    api(project(":feature:home:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:bloggers:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":feature:watching:domain"))

    implementation(libs.bundles.compose.presentation)

    testImplementation(libs.bundles.unit.test)
}
