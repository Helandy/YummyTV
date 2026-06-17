plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.details.presentation"
}

dependencies {
    api(project(":feature:details:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:library:domain"))
    implementation(project(":feature:player:api"))

    implementation(libs.bundles.compose.presentation)

    testImplementation(libs.bundles.unit.test)
}
