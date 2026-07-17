plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.collection.presentation"
}

dependencies {
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:collection:api"))

    api(project(":feature:collection:domain"))

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
