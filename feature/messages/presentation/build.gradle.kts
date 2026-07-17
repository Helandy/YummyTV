plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.messages.presentation" }

dependencies {
    api(project(":feature:messages:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:messages:api"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)
}
