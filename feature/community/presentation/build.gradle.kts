plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.feature.community.presentation" }

dependencies {
    api(project(":feature:posts:domain"))
    api(project(":feature:bloggers:domain"))
    implementation(project(":feature:community:api"))
    implementation(project(":feature:posts:api"))
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:messages:api"))
    implementation(project(":feature:pages:api"))
    implementation(project(":feature:glossary:api"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":core:error:api"))
    implementation(project(":core:model"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(libs.bundles.compose.presentation)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test)
}
