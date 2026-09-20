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
    implementation(project(":core:error:api"))
    implementation(project(":core:model"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:library:domain"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:watching:domain"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)
    implementation(libs.coil.core)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.unit.test)
}
