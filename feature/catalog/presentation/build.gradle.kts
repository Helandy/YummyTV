plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.catalog.presentation"
}

dependencies {
    implementation(project(":feature:catalog:api"))
    api(project(":feature:catalog:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:api"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)

    testImplementation(libs.bundles.unit.test)

    testImplementation(project(":core:testing"))
}
