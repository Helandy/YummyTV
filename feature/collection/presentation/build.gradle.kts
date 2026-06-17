plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.collection.presentation"
}

dependencies {
    implementation(project(":feature:collection:api"))

    api(project(":feature:collection:domain"))
    api(project(":feature:details:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:api"))

    implementation(libs.bundles.compose.presentation)

    testImplementation(libs.bundles.unit.test)
}
