plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.search.presentation"
}

dependencies {
    api(project(":feature:details:domain"))
    api(project(":feature:search:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:api"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)

    testImplementation(libs.bundles.unit.test)
}
