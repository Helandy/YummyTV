plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.bloggers.presentation" }

dependencies {
    implementation(project(":feature:comments:api"))
    api(project(":feature:bloggers:domain"))
    implementation(project(":feature:bloggers:api"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.bundles.compose.presentation)

    testImplementation(project(":core:testing"))
}
